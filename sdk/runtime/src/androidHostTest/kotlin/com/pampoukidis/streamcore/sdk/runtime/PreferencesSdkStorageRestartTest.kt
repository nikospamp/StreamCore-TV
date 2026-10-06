package com.pampoukidis.streamcore.sdk.runtime

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories
import com.pampoukidis.streamcore.sdk.runtime.storage.accountStorageKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.io.File
import java.nio.file.Files
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PreferencesSdkStorageRestartTest {
    @Test
    fun savedDataSurvivesClosingAndReopeningFilesWithoutCrossingPartitions() {
        runTest {
            val directory = Files.createTempDirectory("streamcore-sdk-restart").toFile()
            val configuration = StreamCoreConfiguration("tmdb:test", "saved-data")
            val partitions = listOf(
                accountStorageKey(configuration, "λογαριασμός", "日本語"),
                accountStorageKey(configuration, "another-account", "日本語"),
                accountStorageKey(configuration, "λογαριασμός", "another-profile"),
            )
            var opened = open(directory)
            try {
                partitions.forEachIndexed { index, partition ->
                    val content = content("Ταινία $index")
                    assertIs<StreamCoreResult.Success<*>>(
                        opened.repositories.library.setLiked(partition, content, true, 1_725_000_000_123L + index),
                    )
                    opened.repositories.history.add(partition, content.title)
                    opened.repositories.progress.upsert(progress(partition, content, index))
                }
                opened.job.cancelAndJoin()
                listOf("library", "recent_searches", "playback_progress").forEach { name ->
                    assertTrue(File(directory, "$name.preferences_pb").length() > 0)
                }
                opened = open(directory)
                partitions.forEachIndexed { index, partition ->
                    val content = content("Ταινία $index")
                    val library = assertIs<StreamCoreResult.Success<*>>(opened.repositories.library.observe(partition).first()).value
                    assertEquals(
                        listOf(StreamCoreLibraryEntry(content, 1_725_000_000_123L + index)),
                        library,
                    )
                    assertEquals(listOf(content.title), opened.repositories.history.observe(partition).first())
                    assertEquals(listOf(progress(partition, content, index)), opened.repositories.progress.observe(partition).first())
                }
            } finally {
                opened.job.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun malformedSavedDataRemainsUnchangedAndReportsFailureAfterRestart() {
        runTest {
            val directory = Files.createTempDirectory("streamcore-sdk-malformed").toFile()
            val configuration = StreamCoreConfiguration("tmdb:test", "saved-data")
            val original = "{malformed saved JSON 日本語"
            val rawKey = stringPreferencesKey("recent_searches_json")
            val partition = accountStorageKey(configuration, "account", "profile")
            var opened = open(directory)
            try {
                opened.search.edit { it[rawKey] = original }
                opened.job.cancelAndJoin()
                opened = open(directory)
                assertFailsWith<SerializationException> { opened.repositories.history.observe(partition).first() }
                assertFailsWith<SerializationException> { opened.repositories.history.add(partition, "New movie") }
                assertEquals(original, opened.search.data.first()[rawKey])
            } finally {
                opened.job.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
    }

    private fun content(title: String): StreamCoreContent {
        return StreamCoreContent(
            id = "shared-content", title = title, description = "", rating = 0,
            pgRatingName = "", pgRatingLevel = 0, poster = "", backdrop = null,
            cast = emptyList(), releaseDate = 0L, genres = emptyList(),
        )
    }

    private fun progress(partition: String, content: StreamCoreContent, index: Int): StreamCorePlaybackProgressEntry {
        return StreamCorePlaybackProgressEntry(
            profileId = partition, contentId = content.id, contentSnapshot = content,
            positionMillis = 40_000L + index, durationMillis = 100_000L,
            updatedAtMillis = 1_725_000_000_456L + index,
        )
    }

    private fun open(directory: File): OpenStorage {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        fun store(name: String): DataStore<Preferences> {
            // Android FileStorage relies on POSIX rename replacement, unavailable to File.renameTo
            // on the Windows host. Okio keeps the real Preferences binary codec and atomic writes;
            // the actual Android factory is covered by the device/application smoke journey.
            return DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { File(directory, name).absolutePath.toPath() },
                ),
                scope = scope,
            )
        }
        val search = store("recent_searches.preferences_pb")
        val repositories = PreferencesSdkStorage.create(
            libraryStore = store("library.preferences_pb"), searchStore = search,
            progressStore = store("playback_progress.preferences_pb"),
            json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
        )
        return OpenStorage(job, search, repositories)
    }
    private data class OpenStorage(
        val job: kotlinx.coroutines.CompletableJob,
        val search: DataStore<Preferences>,
        val repositories: SdkLocalRepositories,
    )
}
