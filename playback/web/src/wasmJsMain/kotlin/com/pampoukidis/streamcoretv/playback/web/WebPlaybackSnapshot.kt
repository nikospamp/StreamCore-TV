package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode

internal data class WebPlaybackSnapshot(
    val phase: WebPlaybackPhase = WebPlaybackPhase.Idle,
    val isPlaying: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val videoAspectRatio: Float? = null,
    val speed: Float = 1f,
    val resizeMode: PlaybackResizeMode = PlaybackResizeMode.Fit,
    val videoTracks: List<WebPlaybackTrackSnapshot> = emptyList(),
    val audioTracks: List<WebPlaybackTrackSnapshot> = emptyList(),
    val textTracks: List<WebPlaybackTrackSnapshot> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val selectedTextTrackId: String? = null,
    val failure: WebPlaybackFailure? = null,
)
