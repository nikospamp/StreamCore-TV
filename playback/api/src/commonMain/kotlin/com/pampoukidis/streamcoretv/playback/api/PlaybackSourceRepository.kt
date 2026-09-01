package com.pampoukidis.streamcoretv.playback.api

interface PlaybackSourceRepository {
    suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel
}