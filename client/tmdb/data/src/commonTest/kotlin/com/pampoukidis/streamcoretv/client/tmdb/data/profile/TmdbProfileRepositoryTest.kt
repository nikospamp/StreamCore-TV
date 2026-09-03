package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TmdbProfileRepositoryTest {

    @Test
    fun createUpdateDeleteAndMonotonicIdSurviveRepositoryRecreation() = runTest {
        val dataStore = TestPreferencesDataStore()

        val created = repository(dataStore).createProfile(createProfile(displayName = "First")).successValue()
        assertEquals("tmdb-profile-created-1", created.id)

        val updated = repository(dataStore).updateProfile(
            UpdateProfileModel(
                profileId = created.id,
                displayName = "Updated",
                avatarId = "tmdb-avatar-02",
                parentalLevelId = "teen",
            ),
        ).successValue()
        assertEquals("Updated", updated.displayName)
        assertEquals("tmdb-avatar-02", updated.avatar.id)
        assertEquals("teen", updated.parentalLevel.id)

        repository(dataStore).deleteProfile(created.id).successValue()
        val second = repository(dataStore).createProfile(createProfile(displayName = "Second")).successValue()

        assertEquals("tmdb-profile-created-2", second.id)
        val recreatedProfiles = repository(dataStore).getProfiles().successValue()
        assertTrue(recreatedProfiles.none { it.id == created.id })
        assertEquals("Second", recreatedProfiles.single { it.id == second.id }.displayName)
    }

    @Test
    fun defaultsAreSeededOnlyWhenNoSnapshotExists() = runTest {
        val emptySnapshot = Json.encodeToString(
            TmdbProfilesPreferences(
                schemaVersion = TMDB_PROFILES_SCHEMA_VERSION,
                nextProfileNumber = 1L,
                profiles = emptyList(),
            ),
        )
        val existingStore = TestPreferencesDataStore(
            mutablePreferencesOf(profilesKey() to emptySnapshot),
        )

        assertTrue(repository(existingStore).getProfiles().successValue().isEmpty())

        val newStore = TestPreferencesDataStore()
        val defaults = repository(newStore).getProfiles().successValue()
        assertEquals(listOf("tmdb-profile-owner", "tmdb-profile-kids"), defaults.map { it.id })
        assertTrue(newStore.current()[profilesKey()] != null)
    }

    @Test
    fun malformedSnapshotReturnsParsingErrorWithoutWipingStoredBytes() = runTest {
        val malformed = "{not-valid-json"
        val dataStore = TestPreferencesDataStore(
            mutablePreferencesOf(profilesKey() to malformed),
        )

        val readFailure = assertIs<AppResult.Failure>(repository(dataStore).getProfiles())
        val mutationFailure = assertIs<AppResult.Failure>(
            repository(dataStore).createProfile(createProfile(displayName = "Ignored")),
        )

        assertIs<AppError.Parsing>(readFailure.error)
        assertIs<AppError.Parsing>(mutationFailure.error)
        assertEquals(malformed, dataStore.current()[profilesKey()])
    }

    @Test
    fun concurrentRepositoriesSerializeCreatesWithoutLosingProfilesOrReusingIds() = runTest {
        val dataStore = TestPreferencesDataStore()
        val firstRepository = repository(dataStore)
        val secondRepository = repository(dataStore)

        val created = (1..40).map { index ->
            async {
                val target = if (index % 2 == 0) firstRepository else secondRepository
                target.createProfile(createProfile(displayName = "Profile $index")).successValue()
            }
        }.awaitAll()

        val createdIds = created.map { profile -> profile.id }
        assertEquals(40, createdIds.distinct().size)
        assertEquals(
            (1L..40L).toList(),
            createdIds.map { id -> id.substringAfterLast('-').toLong() }.sorted(),
        )
        assertEquals(42, repository(dataStore).getProfiles().successValue().size)
    }

    @Test
    fun configuredAccountsRemainIsolatedWithinOneDataStore() = runTest {
        val dataStore = TestPreferencesDataStore()
        val accountA = repository(dataStore, accountId = "account-a")
        val accountB = repository(dataStore, accountId = "account-b")

        val createdA = accountA.createProfile(createProfile(displayName = "Account A")).successValue()
        assertTrue(accountB.getProfiles().successValue().none { profile -> profile.id == createdA.id })

        val createdB = accountB.createProfile(createProfile(displayName = "Account B")).successValue()
        accountB.updateProfile(
            UpdateProfileModel(
                profileId = createdB.id,
                displayName = "Account B updated",
                avatarId = "tmdb-avatar-02",
                parentalLevelId = "teen",
            ),
        ).successValue()

        val recreatedAProfiles = repository(dataStore, accountId = "account-a")
            .getProfiles()
            .successValue()
        val recreatedBProfiles = repository(dataStore, accountId = "account-b")
            .getProfiles()
            .successValue()

        assertEquals(
            "Account A",
            recreatedAProfiles.single { profile -> profile.id == createdA.id }.displayName,
        )
        assertTrue(recreatedAProfiles.none { profile -> profile.displayName == "Account B updated" })
        assertEquals(
            "Account B updated",
            recreatedBProfiles.single { profile -> profile.id == createdB.id }.displayName,
        )
        assertTrue(dataStore.current()[profilesKey("account-a")] != null)
        assertTrue(dataStore.current()[profilesKey("account-b")] != null)
    }

    @Test
    fun blankConfiguredAccountConstructsAndSeedsDeterministicUnconfiguredScope() = runTest {
        val dataStore = TestPreferencesDataStore()

        val profiles = repository(dataStore, accountId = "").getProfiles().successValue()

        assertEquals(listOf("tmdb-profile-owner", "tmdb-profile-kids"), profiles.map { profile -> profile.id })
        assertTrue(dataStore.current()[profilesKey(accountId = "")] != null)
        assertEquals(profilesKey(accountId = ""), profilesKey(accountId = "   "))
    }

    @Test
    fun profileMutationsDoNotOwnOrAlterSelectedProfilePreference() = runTest {
        val selectedProfileKey = stringPreferencesKey("web_selected_profile_id")
        val dataStore = TestPreferencesDataStore(
            mutablePreferencesOf(selectedProfileKey to "coordinator-owned-profile"),
        )

        val created = repository(dataStore).createProfile(createProfile(displayName = "Created")).successValue()
        repository(dataStore).selectProfile(created.id).successValue()
        repository(dataStore).deleteProfile(created.id).successValue()

        assertEquals("coordinator-owned-profile", dataStore.current()[selectedProfileKey])
    }

    @Test
    fun returnedProfileListCannotMutatePersistedState() = runTest {
        val dataStore = TestPreferencesDataStore()
        val returned = repository(dataStore).getProfiles().successValue()

        (returned as MutableList<ProfileModel>).clear()

        assertEquals(2, repository(dataStore).getProfiles().successValue().size)
    }

    @Test
    fun cancellationFromDataStorePropagates() = runTest {
        val cancellation = CancellationException("cancelled")
        val repository = repository(CancellingPreferencesDataStore(cancellation))

        val thrown = assertFailsWith<CancellationException> {
            repository.createProfile(createProfile(displayName = "Cancelled"))
        }

        assertEquals(cancellation, thrown)
    }

    private fun repository(
        dataStore: DataStore<Preferences>,
        accountId: String = FIXTURE_ACCOUNT_ID,
    ): TmdbProfileRepository {
        return TmdbProfileRepository(
            dataStore = dataStore,
            json = Json { ignoreUnknownKeys = true },
            accountId = accountId,
        )
    }

    private fun profilesKey(accountId: String = FIXTURE_ACCOUNT_ID): Preferences.Key<String> {
        return tmdbProfilesPreferencesKey(accountId)
    }

    private fun createProfile(displayName: String): CreateProfileModel {
        return CreateProfileModel(
            displayName = displayName,
            avatarId = "tmdb-avatar-01",
            parentalLevelId = "all",
        )
    }

    private fun <T> AppResult<T>.successValue(): T {
        return (this as AppResult.Success).value
    }

    private class TestPreferencesDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            return mutex.withLock {
                transform(state.value).also { updated -> state.value = updated }
            }
        }

        fun current(): Preferences {
            return state.value
        }
    }

    private class CancellingPreferencesDataStore(
        private val cancellation: CancellationException,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            throw cancellation
        }
    }

    private companion object {
        const val FIXTURE_ACCOUNT_ID = "fixture-account"
    }
}
