@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcore.sdk.runtime

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.WebLocalStorage
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
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Clock

class BrowserPreferencesRestartTest {
    @Test
    fun browserSavedDataSurvivesRestartWithoutCrossingPartitions(): TestResult {
        return runTest {
            if (!hasBrowserStorage()) {
                println("SKIPPED browser persistence: this Node execution has no browser localStorage; run wasmJsBrowserTest.")
                return@runTest
            }
            val prefix = "streamcore_sdk_test_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt().toUInt()}"
            val configuration = StreamCoreConfiguration("browser:test", prefix)
            val partitions = listOf(
                accountStorageKey(configuration, "λογαριασμός", "日本語"),
                accountStorageKey(configuration, "another-account", "日本語"),
                accountStorageKey(configuration, "λογαριασμός", "another-profile"),
            )
            var opened = open(prefix)
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
                opened = open(prefix)
                partitions.forEachIndexed { index, partition ->
                    val content = content("Ταινία $index")
                    val library = assertIs<StreamCoreResult.Success<*>>(opened.repositories.library.observe(partition).first()).value
                    assertEquals(listOf(StreamCoreLibraryEntry(content, 1_725_000_000_123L + index)), library)
                    assertEquals(listOf(content.title), opened.repositories.history.observe(partition).first())
                    assertEquals(listOf(progress(partition, content, index)), opened.repositories.progress.observe(partition).first())
                }
            } finally {
                opened.job.cancelAndJoin()
                clean(prefix)
            }

        }
    }

    @Test
    fun malformedSavedDataRemainsUnchangedAndReportsFailureAfterRestart(): TestResult {
        return runTest {
            if (!hasBrowserStorage()) {
                println("SKIPPED browser persistence: this Node execution has no browser localStorage; run wasmJsBrowserTest.")
                return@runTest
            }
            val prefix = "streamcore_sdk_test_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt().toUInt()}"
            val configuration = StreamCoreConfiguration("browser:test", prefix)
            val partition = accountStorageKey(configuration, "account", "profile")
            val original = "{malformed saved JSON 日本語"
            val rawKey = stringPreferencesKey("recent_searches_json")
            var opened = open(prefix)
            try {
                opened.search.edit { it[rawKey] = original }
                opened.job.cancelAndJoin()
                opened = open(prefix)
                assertFailsWith<SerializationException> { opened.repositories.history.observe(partition).first() }
                assertFailsWith<SerializationException> { opened.repositories.history.add(partition, "New movie") }
                assertEquals(original, opened.search.data.first()[rawKey])
            } finally {
                opened.job.cancelAndJoin()
                clean(prefix)
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

    private fun open(prefix: String): OpenStorage {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Default)
        fun store(suffix: String): DataStore<Preferences> {
            return DataStoreFactory.create(storage = WebLocalStorage(serializer = PreferencesSerializer, name = prefix + suffix), scope = scope)
        }
        val search = store("_search")
        val repositories = PreferencesSdkStorage.create(store("_library"), search, store("_progress"), Json { ignoreUnknownKeys = true; encodeDefaults = true })
        return OpenStorage(job, search, repositories)
    }
    private fun clean(prefix: String) {
        // Only the exact names allocated by this test; never clear a browser's shared storage.
        listOf("_search", "_library", "_progress").forEach { suffix -> removeTestStorage(prefix + suffix) }
    }
    private data class OpenStorage(
        val job: kotlinx.coroutines.CompletableJob,
        val search: DataStore<Preferences>,
        val repositories: SdkLocalRepositories,
    )
}

@JsFun("() => typeof window !== 'undefined' && typeof localStorage !== 'undefined'")
private external fun hasBrowserStorage(): Boolean

@JsFun("(name) => { localStorage.removeItem(name); }")
private external fun removeTestStorage(name: String)
