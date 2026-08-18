package com.pampoukidis.streamcoretv.playback.api

data class PlaybackEngineState(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val isPlaying: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val videoAspectRatio: Float? = null,
    val speed: Float = 1f,
    val resizeMode: PlaybackResizeMode = PlaybackResizeMode.Fit,
    val videoTracks: List<PlaybackTrackModel> = emptyList(),
    val audioTracks: List<PlaybackTrackModel> = emptyList(),
    val textTracks: List<PlaybackTrackModel> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val selectedTextTrackId: String? = null,
    val error: PlaybackErrorModel? = null,
)