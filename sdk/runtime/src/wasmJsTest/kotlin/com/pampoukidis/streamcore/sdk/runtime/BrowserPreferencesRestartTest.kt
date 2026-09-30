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
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock

class BrowserPreferencesRestartTest {
    @Test
    fun browserLocalStoragePreservesOwnedAndQuarantinedDataAcrossRestart(): TestResult {
        return runTest {
            if (!hasBrowserStorage()) {
                println("SKIPPED browser persistence: this Node execution has no browser localStorage; run wasmJsBrowserTest.")
                return@runTest
            }
            val prefix = "streamcore_sdk_test_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt().toUInt()}"
            val configuration = StreamCoreConfiguration("browser:test", prefix)
            val account = "λογαριασμός"
            val profile = "日本語"
            val original = """{"queriesByProfile":{"日本語":["Ταινία"]}}"""
            var opened = open(prefix)
            try {
                opened.search.edit { it[stringPreferencesKey("recent_searches_json")] = original }
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, account))
                opened.job.cancelAndJoin()
                opened = open(prefix)
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, account))
                assertEquals(listOf("Ταινία"), opened.repositories.history.observe(accountStorageKey(configuration, account, profile)).first())
                assertEquals(original, opened.search.data.first()[stringPreferencesKey("sdk_v1_backup_recent_searches_json")])
            } finally {
                opened.job.cancelAndJoin()
                clean(prefix)
            }

            val unknownPrefix = prefix + "_unknown"
            val unownedBytes = "{malformed legacy data 日本語 retained verbatim"
            opened = open(unknownPrefix)
            try {
                opened.search.edit { it[stringPreferencesKey("recent_searches_json")] = unownedBytes }
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, null))
                val newPartition = accountStorageKey(configuration, "new-account", profile)
                opened.repositories.history.add(newPartition, "New movie")
                opened.job.cancelAndJoin()
                opened = open(unknownPrefix)
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, "new-account"))
                assertEquals(listOf("New movie"), opened.repositories.history.observe(newPartition).first())
                assertEquals(unownedBytes, opened.search.data.first()[stringPreferencesKey("sdk_v2_unowned_recent_searches_json")])
            } finally {
                opened.job.cancelAndJoin()
                clean(unknownPrefix)
            }
        }
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
        val repositories: com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories,
    )
}

@JsFun("() => typeof window !== 'undefined' && typeof localStorage !== 'undefined'")
private external fun hasBrowserStorage(): Boolean

@JsFun("(name) => { localStorage.removeItem(name); }")
private external fun removeTestStorage(name: String)
