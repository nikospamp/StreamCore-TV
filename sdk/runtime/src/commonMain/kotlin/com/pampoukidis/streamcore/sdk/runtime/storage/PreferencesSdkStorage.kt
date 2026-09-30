package com.pampoukidis.streamcore.sdk.runtime.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.runtime.storage.library.PreferencesLibraryRepository
import com.pampoukidis.streamcore.sdk.runtime.storage.migration.migrateLegacyPreferences
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PreferencesPlaybackProgressRepository
import com.pampoukidis.streamcore.sdk.runtime.storage.search.PreferencesRecentSearchRepository
import com.pampoukidis.streamcore.sdk.runtime.accountStorageKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Adapter used only by platform/provider factories; application features never see DataStore. */
object PreferencesSdkStorage {
    fun create(
        libraryStore: DataStore<Preferences>,
        searchStore: DataStore<Preferences>,
        progressStore: DataStore<Preferences>,
        json: Json,
        authStore: DataStore<Preferences>? = null,
    ): SdkLocalRepositories {
        val contextStore = authStore?.let(::ReportingPreferencesStore)
        return SdkLocalRepositories(
            library = PreferencesLibraryRepository(ReportingPreferencesStore(libraryStore), json),
            history = PreferencesRecentSearchRepository(ReportingPreferencesStore(searchStore), json, reportFailures = true),
            progress = PreferencesPlaybackProgressRepository(ReportingPreferencesStore(progressStore), json, reportFailures = true),
            loadSelectedProfile = { configuration, account ->
                val values = contextStore?.data?.first()
                val scoped = stringPreferencesKey("sdk_selected_profile." + accountStorageKey(configuration, account, ""))
                val legacySuffix = account.encodeToByteArray().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
                values?.get(scoped) ?: values?.get(stringPreferencesKey("web_selected_profile_id.$legacySuffix"))
            },
            saveSelectedProfile = { configuration, account, profile ->
                contextStore?.edit { values ->
                    val scoped = stringPreferencesKey("sdk_selected_profile." + accountStorageKey(configuration, account, ""))
                    // Empty sentinel prevents accidentally reviving a legacy selection after clearing it.
                    values[scoped] = profile.orEmpty()
                }
            },
            migrateLegacy = { configuration, trustedOwner ->
                try {
                    migrateLegacyPreferences(libraryStore, json, "library_json", "entriesByProfile", configuration, trustedOwner)
                    migrateLegacyPreferences(searchStore, json, "recent_searches_json", "queriesByProfile", configuration, trustedOwner)
                    migrateLegacyPreferences(progressStore, json, "entries_json", null, configuration, trustedOwner)
                    // Validate the credential/profile-context store during explicit bootstrap too.
                    // This lets browser hosts select their existing session-storage fallback before login.
                    contextStore?.edit { values ->
                        values[stringPreferencesKey("sdk_context_schema")] = "2"
                    }
                    StreamCoreResult.Success(Unit)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: SerializationException) { StreamCoreResult.Failure(StreamCoreError.Parsing()) }
                catch (_: Exception) { StreamCoreResult.Failure(StreamCoreError.Storage()) }
            },
        )
    }
}
