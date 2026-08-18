package com.pampoukidis.streamcoretv.feature.player.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesPlaybackProgressRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `entries are serialized ordered and isolated by profile`() = runTest {
        val repository = repository("ordered")
        repository.upsert(entry("profile-a", "older", 31_000L, updatedAt = 1L))
        repository.upsert(entry("profile-b", "other", 32_000L, updatedAt = 3L))
        repository.upsert(entry("profile-a", "newer", 33_000L, updatedAt = 2L))

        assertEquals(listOf("newer", "older"), repository.observe("profile-a").first().map { it.contentId })
        assertEquals(listOf("other"), repository.observe("profile-b").first().map { it.contentId })
    }

    @Test
    fun `profile retains newest fifty entries`() = runTest {
        val repository = repository("cap")
        repeat(55) { index ->
            repository.upsert(entry("profile", "content-$index", 31_000L, updatedAt = index.toLong()))
        }

        val values = repository.observe("profile").first()
        assertEquals(50, values.size)
        assertEquals("content-54", values.first().contentId)
        assertEquals("content-5", values.last().contentId)
    }

    @Test
    fun `short unknown and completed positions remove progress`() = runTest {
        val repository = repository("thresholds")
        repository.upsert(entry("profile", "content", 40_000L))
        repository.upsert(entry("profile", "content", 29_999L))
        assertNull(repository.get("profile", "content"))

        repository.upsert(entry("profile", "content", 40_000L))
        repository.upsert(entry("profile", "content", 95_000L))
        assertNull(repository.get("profile", "content"))

        repository.upsert(entry("profile", "content", 40_000L, duration = 0L))
        assertNull(repository.get("profile", "content"))
    }

    private fun kotlinx.coroutines.test.TestScope.repository(name: String): PreferencesPlaybackProgressRepository {
        val file = File(temporaryFolder.newFolder(name), "progress.preferences_pb")
        return PreferencesPlaybackProgressRepository(
            dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { file },
            ),
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
            contentSnapshot = ContentModel(
                id = contentId,
                title = contentId,
                description = "",
                rating = 0,
                pgRatingName = "",
                pgRatingLevel = 0,
                poster = "",
                backdrop = null,
                cast = emptyList(),
                releaseDate = 0L,
                genres = emptyList(),
            ),
            positionMillis = position,
            durationMillis = duration,
            updatedAtMillis = updatedAt,
        )
    }
}
