package com.pampoukidis.streamcore.sdk.runtime.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.runtime.storage.library.PreferencesLibraryRepository
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PreferencesPlaybackProgressRepository
import com.pampoukidis.streamcore.sdk.runtime.storage.search.PreferencesRecentSearchRepository
import kotlinx.coroutines.CancellationException
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
        val contextStore = authStore?.let(::ErrorMappingPreferencesStore)
        return SdkLocalRepositories(
            library = PreferencesLibraryRepository(ErrorMappingPreferencesStore(libraryStore), json),
            history = PreferencesRecentSearchRepository(ErrorMappingPreferencesStore(searchStore), json, reportFailures = true),
            progress = PreferencesPlaybackProgressRepository(ErrorMappingPreferencesStore(progressStore), json, reportFailures = true),
            saveSelectedProfile = { configuration, account, profile ->
                contextStore?.edit { values ->
                    val scoped = stringPreferencesKey("sdk_selected_profile." + accountStorageKey(configuration, account, ""))
                    // An empty value records that this account has no selected profile.
                    values[scoped] = profile.orEmpty()
                }
            },
            checkContextStorage = {
                try {
                    // Check context writes before authentication so browser hosts can offer their
                    // existing session-storage fallback when persistent storage is unavailable.
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
