package com.pampoukidis.streamcoretv.feature.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PreferencesPlaybackProgressRepositoryTest {

    @Test
    fun entriesAreSerializedOrderedAndIsolatedByProfile() = runTest {
        val repository = repository(TestPreferencesDataStore())
        repository.upsert(entry("profile-a", "older", 31_000L, updatedAt = 1L))
        repository.upsert(entry("profile-b", "other", 32_000L, updatedAt = 3L))
        repository.upsert(entry("profile-a", "newer", 33_000L, updatedAt = 2L))

        assertEquals(listOf("newer", "older"), repository.observe("profile-a").first().map { it.contentId })
        assertEquals(listOf("other"), repository.observe("profile-b").first().map { it.contentId })
    }

    @Test
    fun profileRetainsNewestFiftyEntries() = runTest {
        val repository = repository(TestPreferencesDataStore())
        repeat(55) { index ->
            repository.upsert(entry("profile", "content-$index", 31_000L, updatedAt = index.toLong()))
        }

        val values = repository.observe("profile").first()
        assertEquals(50, values.size)
        assertEquals("content-54", values.first().contentId)
        assertEquals("content-5", values.last().contentId)
    }

    @Test
    fun shortUnknownAndCompletedPositionsRemoveProgress() = runTest {
        val repository = repository(TestPreferencesDataStore())
        repository.upsert(entry("profile", "content", 40_000L))
        repository.upsert(entry("profile", "content", 29_999L))
        assertNull(repository.get("profile", "content"))

        repository.upsert(entry("profile", "content", 40_000L))
        repository.upsert(entry("profile", "content", 95_000L))
        assertNull(repository.get("profile", "content"))

        repository.upsert(entry("profile", "content", 40_000L, duration = 0L))
        assertNull(repository.get("profile", "content"))
    }

    @Test
    fun preMigrationKeyJsonAndEpochMillisecondsRemainReadableAfterRecreation() = runTest {
        val dataStore = TestPreferencesDataStore(
            mutablePreferencesOf(EntriesKey to LegacyProgressJson),
        )

        val first = repository(dataStore).get("profile", "legacy")
        val recreated = repository(dataStore).get("profile", "legacy")

        assertEquals(40_000L, first?.positionMillis)
        assertEquals(1_725_000_000_123L, first?.updatedAtMillis)
        assertEquals(first, recreated)
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

    private fun repository(dataStore: DataStore<Preferences>): PreferencesPlaybackProgressRepository {
        return PreferencesPlaybackProgressRepository(
            dataStore = dataStore,
            json = Json { encodeDefaults = true },
        )
    }

    private fun entry(
        profileId: String,
        contentId: String,
        position: Long,
        duration: Long = 100_000L,
        updatedAt: Long = 1L,
    ): PlaybackProgressEntryModel {
        return PlaybackProgressEntryModel(
            profileId = profileId,
            contentId = contentId,
            contentSnapshot = content(contentId),
            positionMillis = position,
            durationMillis = duration,
            updatedAtMillis = updatedAt,
        )
    }

    private fun content(id: String): ContentModel {
        return ContentModel(
            id = id,
            title = id,
            description = "",
            rating = 0,
            pgRatingName = "",
            pgRatingLevel = 0,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
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

    private companion object {
        val EntriesKey = stringPreferencesKey("entries_json")
        const val LegacyProgressJson =
            "[{\"profileId\":\"profile\",\"contentId\":\"legacy\",\"contentSnapshot\":{" +
                "\"id\":\"legacy\",\"title\":\"Legacy\",\"description\":\"\",\"rating\":0," +
                "\"pgRatingName\":\"\",\"pgRatingLevel\":0,\"poster\":\"\",\"backdrop\":null," +
                "\"cast\":[],\"releaseDate\":0,\"genres\":[]},\"positionMillis\":40000," +
                "\"durationMillis\":100000,\"updatedAtMillis\":1725000000123}]"
    }
}
