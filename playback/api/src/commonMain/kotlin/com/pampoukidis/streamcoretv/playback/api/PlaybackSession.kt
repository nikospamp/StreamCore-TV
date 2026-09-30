package com.pampoukidis.streamcoretv.playback.api

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlaybackSession : AutoCloseable {
    val state: StateFlow<PlaybackEngineState>
    val videoSurface: PlaybackVideoSurface

    fun prepare(media: StreamCorePlaybackMedia, startPositionMillis: Long)
    fun play()
    fun pause()
    fun seekTo(positionMillis: Long)
    fun setSpeed(speed: Float)
    fun selectVideoTrack(trackId: String?)
    fun selectAudioTrack(trackId: String?)
    fun selectTextTrack(trackId: String?)
    fun setResizeMode(mode: PlaybackResizeMode)
    fun retry()
    fun requestFilmstrip(positionsMillis: List<Long>): Flow<PlaybackFilmstripFrameModel>
}
