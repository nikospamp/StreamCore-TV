package com.pampoukidis.streamcore.sdk.runtime.storage.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.runtime.storage.accountStorageKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PreferencesRecentSearchRepositoryTest {

    @Test
    fun queriesAreNormalizedDeduplicatedNewestFirstAndCapped() = runTest {
        val repository = repository(TestPreferencesDataStore())
        repository.add("profile-a", " First ")
        repository.add("profile-a", "Second")
        repository.add("profile-a", "first")
        repository.add("profile-a", "Third")
        repository.add("profile-a", "Fourth")
        repository.add("profile-a", "Fifth")
        repository.add("profile-a", "Sixth")

        assertEquals(
            listOf("Sixth", "Fifth", "Fourth", "Third", "first"),
            repository.observe("profile-a").first(),
        )
    }

    @Test
    fun profilesAreIsolatedAndRemoveAndClearAreScoped() = runTest {
        val repository = repository(TestPreferencesDataStore())
        repository.add("profile-a", "Shared")
        repository.add("profile-a", "Only A")
        repository.add("profile-b", "Shared")

        repository.remove("profile-a", "shared")
        assertEquals(listOf("Only A"), repository.observe("profile-a").first())
        assertEquals(listOf("Shared"), repository.observe("profile-b").first())

        repository.clear("profile-b")
        assertEquals(emptyList(), repository.observe("profile-b").first())
    }

    @Test
    fun accountPartitionAndQueriesSurviveRepositoryRecreation(): TestResult {
        return runTest {
            val dataStore = TestPreferencesDataStore()
            val configuration = StreamCoreConfiguration("test:catalogue", "saved-data")
            val partition = accountStorageKey(configuration, "λογαριασμός", "日本語")
            val original = repository(dataStore)
            original.add(partition, "Older query")
            original.add(partition, "Ταινία")

            assertEquals(
                listOf("Ταινία", "Older query"),
                repository(dataStore).observe(partition).first(),
            )
        }
    }

    @Test
    fun storageFailureRecoversButCancellationIsRethrown() = runTest {
        assertEquals(
            emptyList(),
            repository(FailingDataStore(StorageFailure())).observe("profile").first(),
        )
        assertFailsWith<CancellationException> {
            repository(FailingDataStore(CancellationException("cancelled")))
                .observe("profile")
                .first()
        }
    }

    private fun repository(dataStore: DataStore<Preferences>): PreferencesRecentSearchRepository {
        return PreferencesRecentSearchRepository(
            dataStore = dataStore,
            json = Json { encodeDefaults = true },
        )
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
    }

    private class FailingDataStore(
        private val throwable: Throwable,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw throwable }

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            throw throwable
        }
    }

    private class StorageFailure : RuntimeException()

}
