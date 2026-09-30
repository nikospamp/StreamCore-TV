package com.pampoukidis.streamcore.sdk.runtime.storage.playback

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import kotlinx.coroutines.flow.Flow

interface PlaybackProgressStore {
    fun observe(profileId: String): Flow<List<StreamCorePlaybackProgressEntry>>
    suspend fun get(profileId: String, contentId: String): StreamCorePlaybackProgressEntry?
    suspend fun upsert(entry: StreamCorePlaybackProgressEntry)
    suspend fun remove(profileId: String, contentId: String)
}