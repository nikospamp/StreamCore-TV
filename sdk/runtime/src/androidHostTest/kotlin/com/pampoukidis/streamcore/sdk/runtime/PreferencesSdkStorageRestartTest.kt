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
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PreferencesSdkStorageRestartTest {
    @Test
    fun binaryDataStoreMigrationSurvivesClosingAndReopeningFiles() {
        runTest {
            val directory = Files.createTempDirectory("streamcore-sdk-restart").toFile()
            val configuration = StreamCoreConfiguration("tmdb:test", "legacy")
            val account = "λογαριασμός"
            val profile = "日本語"
            val original = """{"queriesByProfile":{"日本語":["Ταινία"]}}"""
            val rawKey = stringPreferencesKey("recent_searches_json")
            var opened = open(directory)
            try {
                opened.search.edit { it[rawKey] = original }
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, account))
                assertEquals(listOf("Ταινία"), opened.repositories.history.observe(accountStorageKey(configuration, account, profile)).first())
                opened.job.cancelAndJoin()
                assertTrue(File(directory, "recent_searches.preferences_pb").length() > 0)
                opened = open(directory)
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, account))
                assertEquals(listOf("Ταινία"), opened.repositories.history.observe(accountStorageKey(configuration, account, profile)).first())
                assertEquals(original, opened.search.data.first()[stringPreferencesKey("sdk_v1_backup_recent_searches_json")])
                assertEquals(account, opened.search.data.first()[stringPreferencesKey("sdk_v2_migration_owner_recent_searches_json")])
            } finally {
                opened.job.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun loggedOutMalformedBytesAreQuarantinedAndNewDataRemainsUsableAfterRestart() {
        runTest {
            val directory = Files.createTempDirectory("streamcore-sdk-unowned").toFile()
            val configuration = StreamCoreConfiguration("tmdb:test", "legacy")
            val original = "{malformed legacy JSON: preserve these exact bytes 日本語"
            var opened = open(directory)
            try {
                opened.search.edit { it[stringPreferencesKey("recent_searches_json")] = original }
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, null))
                val key = accountStorageKey(configuration, "new-account", "profile")
                opened.repositories.history.add(key, "New movie")
                opened.job.cancelAndJoin()
                opened = open(directory)
                assertIs<StreamCoreResult.Success<*>>(opened.repositories.migrateLegacy(configuration, "new-account"))
                assertEquals(listOf("New movie"), opened.repositories.history.observe(key).first())
                assertEquals(original, opened.search.data.first()[stringPreferencesKey("sdk_v2_unowned_recent_searches_json")])
                assertEquals(original, opened.search.data.first()[stringPreferencesKey("sdk_v1_backup_recent_searches_json")])
            } finally {
                opened.job.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
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
        val repositories: com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories,
    )
}
