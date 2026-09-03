package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode

internal interface WebPlaybackBridge {
    fun setListener(listener: WebPlaybackBridgeListener?)
    fun reset(generation: Int)
    fun load(generation: Int, uri: String, mimeType: String?, startPositionMillis: Long)
    fun play()
    fun pause()
    fun seekTo(positionMillis: Long)
    fun setSpeed(speed: Float)
    fun selectVideoTrack(trackId: String?)
    fun selectAudioTrack(trackId: String?)
    fun selectTextTrack(trackId: String?)
    fun setResizeMode(mode: PlaybackResizeMode)
    fun requestFilmstrip(
        generation: Int,
        positionsMillis: List<Long>,
        listener: WebFilmstripListener,
    ): Int?
    fun cancelFilmstrip(requestId: Int)
    fun close()
}
