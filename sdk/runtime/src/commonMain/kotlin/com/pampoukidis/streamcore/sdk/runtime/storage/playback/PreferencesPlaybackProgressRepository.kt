package com.pampoukidis.streamcore.sdk.runtime.storage.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PlaybackProgressStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal class PreferencesPlaybackProgressRepository constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val reportFailures: Boolean = false,
) : PlaybackProgressStore {

    private val entries: Flow<List<StreamCorePlaybackProgressEntry>> = dataStore.data
        .catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            if (reportFailures) throw throwable
            emit(emptyPreferences())
        }
        .map { preferences -> decode(preferences[EntriesKey]) }

    override fun observe(profileId: String): Flow<List<StreamCorePlaybackProgressEntry>> {
        return entries.map { values ->
            values
                .asSequence()
                .filter { entry -> entry.profileId == profileId }
                .sortedByDescending { entry -> entry.updatedAtMillis }
                .toList()
        }
    }

    override suspend fun get(
        profileId: String,
        contentId: String,
    ): StreamCorePlaybackProgressEntry? {
        return observe(profileId).first().firstOrNull { entry -> entry.contentId == contentId }
    }

    override suspend fun upsert(entry: StreamCorePlaybackProgressEntry) {
        if (!entry.isResumable()) {
            remove(entry.profileId, entry.contentId)
            return
        }

        dataStore.edit { preferences ->
            val current = decode(preferences[EntriesKey])
                .filterNot { existing ->
                    existing.profileId == entry.profileId && existing.contentId == entry.contentId
                }
            val sanitizedEntry = entry.copy(
                contentSnapshot = entry.contentSnapshot.copy(playbackProgress = null),
            )
            val retainedForProfile = (current.filter { it.profileId == entry.profileId } + sanitizedEntry)
                .sortedByDescending { value -> value.updatedAtMillis }
                .take(MaxEntriesPerProfile)
            val otherProfiles = current.filterNot { it.profileId == entry.profileId }
            preferences[EntriesKey] = json.encodeToString(otherProfiles + retainedForProfile)
        }
    }

    override suspend fun remove(profileId: String, contentId: String) {
        dataStore.edit { preferences ->
            val retained = decode(preferences[EntriesKey]).filterNot { entry ->
                entry.profileId == profileId && entry.contentId == contentId
            }
            preferences[EntriesKey] = json.encodeToString(retained)
        }
    }

    private fun decode(encoded: String?): List<StreamCorePlaybackProgressEntry> {
        if (encoded.isNullOrBlank()) {
            return emptyList()
        }

        if (reportFailures) return json.decodeFromString<List<StreamCorePlaybackProgressEntry>>(encoded)

        return runCatching {
            json.decodeFromString<List<StreamCorePlaybackProgressEntry>>(encoded)
        }.getOrDefault(emptyList())
    }

    private fun StreamCorePlaybackProgressEntry.isResumable(): Boolean {
        if (durationMillis <= 0L || positionMillis < MinimumPositionMillis) {
            return false
        }

        return positionMillis.toDouble() / durationMillis.toDouble() < CompletionFraction
    }

    private companion object {
        val EntriesKey = stringPreferencesKey("entries_json")
        const val MinimumPositionMillis = 30_000L
        const val CompletionFraction = 0.95
        const val MaxEntriesPerProfile = 50
    }
}
