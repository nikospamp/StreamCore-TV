package com.pampoukidis.streamcoretv.playback.api

import kotlinx.coroutines.flow.Flow

interface PlaybackProgressRepository {
    fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>>
    suspend fun get(profileId: String, contentId: String): PlaybackProgressEntryModel?
    suspend fun upsert(entry: PlaybackProgressEntryModel)
    suspend fun remove(profileId: String, contentId: String)
}