package com.pampoukidis.streamcoretv.feature.library.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesLibraryRepositoryTest {

    @Test
    fun `memberships are independent ordered and isolated by profile`() = runTest {
        val repository = fixture("memberships").repository
        val shared = content("shared")
        val newest = content("newest")

        val firstMutation = repository.setLiked(
            "profile-a",
            shared,
            isLiked = true,
            changedAtMillis = 10L,
        )
        assertTrue(firstMutation is AppResult.Success, "$firstMutation")
        repository.setInMyList("profile-a", shared, isInMyList = true, changedAtMillis = 20L)
        repository.setLiked("profile-a", newest, isLiked = true, changedAtMillis = 30L)
        repository.setLiked("profile-b", content("other"), isLiked = true, changedAtMillis = 40L)

        val profileA = repository.observe("profile-a").first().successValue()
        assertEquals(listOf("newest", "shared"), profileA.map { entry -> entry.content.id })
        assertEquals(10L, profileA.last().likedAtMillis)
        assertEquals(20L, profileA.last().addedToMyListAtMillis)
        assertEquals(listOf("other"), repository.observe("profile-b").first().successValue().map { it.content.id })

        repository.setLiked("profile-a", shared, isLiked = false, changedAtMillis = 50L)
        val retained = repository.observe("profile-a").first().successValue()
            .first { entry -> entry.content.id == "shared" }
        assertNull(retained.likedAtMillis)
        assertEquals(20L, retained.addedToMyListAtMillis)

        repository.setInMyList("profile-a", shared, isInMyList = false, changedAtMillis = 60L)
        assertEquals(
            listOf("newest"),
            repository.observe("profile-a").first().successValue().map { entry -> entry.content.id },
        )
    }

    @Test
    fun `idempotent add preserves timestamp refreshes compact snapshot and clears transient fields`() = runTest {
        val fixture = fixture("snapshot")
        val repository = fixture.repository
        repository.setLiked(
            profileId = "profile",
            content = content("content", title = "Old"),
            isLiked = true,
            changedAtMillis = 10L,
        )
        repository.setLiked(
            profileId = "profile",
            content = content("content", title = "Updated"),
            isLiked = true,
            changedAtMillis = 99L,
        )

        val entry = repository.observe("profile").first().successValue().single()
        assertEquals(10L, entry.likedAtMillis)
        assertEquals("Updated", entry.content.title)
        assertEquals("", entry.content.description)
        assertTrue(entry.content.cast.isEmpty())
        assertNull(entry.content.row)
        assertNull(entry.content.playbackProgress)
        assertEquals(listOf(Genre(id = "genre", name = "Genre")), entry.content.genres)

        val encoded = fixture.dataStore.data.first()[LibraryJsonKey].orEmpty()
        assertFalse(encoded.contains("Large description"))
        assertFalse(encoded.contains("Actor"))
        assertFalse(encoded.contains("transient-row"))
    }

    @Test
    fun `concurrent membership writes preserve both values`() = runTest {
        val repository = fixture("concurrent").repository
        val content = content("content")

        listOf(
            async {
                repository.setLiked("profile", content, isLiked = true, changedAtMillis = 10L)
            },
            async {
                repository.setInMyList("profile", content, isInMyList = true, changedAtMillis = 20L)
            },
        ).awaitAll()

        val entry = repository.observe("profile").first().successValue().single()
        assertEquals(10L, entry.likedAtMillis)
        assertEquals(20L, entry.addedToMyListAtMillis)
    }

    @Test
    fun `malformed payload reports parsing failure and is not overwritten`() = runTest {
        val fixture = fixture("malformed")
        fixture.dataStore.edit { preferences -> preferences[LibraryJsonKey] = "not-json" }

        val observed = fixture.repository.observe("profile").first()
        assertTrue(observed is AppResult.Failure)
        assertTrue(observed.error is AppError.Parsing)

        val mutation = fixture.repository.setLiked(
            profileId = "profile",
            content = content("content"),
            isLiked = true,
            changedAtMillis = 10L,
        )
        assertTrue(mutation is AppResult.Failure)
        assertTrue(mutation.error is AppError.Parsing)
        assertEquals("not-json", fixture.dataStore.data.first()[LibraryJsonKey])
    }

    @Test
    fun `blank profile is empty and rejects mutations`() = runTest {
        val repository = fixture("invalid").repository

        assertTrue(repository.observe(" ").first().successValue().isEmpty())
        assertTrue(
            repository.setLiked("", content("content"), true, 1L) is AppResult.Failure,
        )
    }

    @Test
    fun `pre migration key json and epoch timestamps remain readable after recreation`() = runTest {
        val dataStore = TestPreferencesDataStore(
            mutablePreferencesOf(LibraryJsonKey to LegacyLibraryJson),
        )

        val first = repository(dataStore).observe("profile").first().successValue().single()
        val recreated = repository(dataStore).observe("profile").first().successValue().single()

        assertEquals("legacy", first.content.id)
        assertEquals(1_725_000_000_123L, first.likedAtMillis)
        assertEquals(1_725_000_000_456L, first.addedToMyListAtMillis)
        assertEquals(first, recreated)
    }

    @Test
    fun `storage failure is recoverable and cancellation is rethrown`() = runTest {
        val failure = repository(FailingDataStore(StorageFailure())).observe("profile").first()
        assertTrue(failure is AppResult.Failure)
        assertTrue(failure.error is AppError.Unknown)

        assertFailsWith<CancellationException> {
            repository(FailingDataStore(CancellationException("cancelled")))
                .observe("profile")
                .first()
        }
    }

    private fun TestScope.fixture(name: String): RepositoryFixture {
        return fixture(TestPreferencesDataStore())
    }

    private fun fixture(dataStore: DataStore<Preferences>): RepositoryFixture {
        return RepositoryFixture(
            repository = repository(dataStore),
            dataStore = dataStore,
        )
    }

    private fun repository(dataStore: DataStore<Preferences>): PreferencesLibraryRepository {
        return PreferencesLibraryRepository(
            dataStore = dataStore,
            json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    private fun content(
        id: String,
        title: String = id,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = title,
            description = "Large description that must not be persisted",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "poster-$id",
            backdrop = "backdrop-$id",
            cast = listOf(
                Cast(
                    id = "cast",
                    name = "Actor",
                    characterName = "Character",
                    image = null,
                ),
            ),
            releaseDate = 1L,
            genres = listOf(Genre(id = "genre", name = "Genre")),
            row = "transient-row",
            playbackProgress = PlaybackProgressModel(
                positionMillis = 40_000L,
                durationMillis = 100_000L,
            ),
        )
    }

    private fun AppResult<List<LibraryEntryModel>>.successValue(): List<LibraryEntryModel> {
        return (this as AppResult.Success).value
    }

    private data class RepositoryFixture(
        val repository: PreferencesLibraryRepository,
        val dataStore: DataStore<Preferences>,
    )

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

    private companion object {
        val LibraryJsonKey = stringPreferencesKey("library_json")
        const val LegacyLibraryJson =
            "{\"version\":1,\"entriesByProfile\":{\"profile\":[{\"content\":{" +
                "\"id\":\"legacy\",\"title\":\"Legacy\",\"rating\":8,\"pgRatingName\":\"PG-13\"," +
                "\"pgRatingLevel\":13,\"poster\":\"poster\",\"backdrop\":null,\"releaseDate\":0," +
                "\"genres\":[]},\"likedAtMillis\":1725000000123," +
                "\"addedToMyListAtMillis\":1725000000456}]}}"
    }
}
