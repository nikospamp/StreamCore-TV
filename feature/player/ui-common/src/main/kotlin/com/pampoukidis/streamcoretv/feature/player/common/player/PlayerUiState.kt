package com.pampoukidis.streamcoretv.feature.player.common.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel

data class PlayerUiState(
    val title: String = "",
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val isPlaying: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val videoAspectRatio: Float? = null,
    val controlsVisible: Boolean = true,
    val isScrubbing: Boolean = false,
    val scrubPositionMillis: Long = 0L,
    val filmstripFrames: List<PlaybackFilmstripFrameModel> = emptyList(),
    val videoTracks: List<PlaybackTrackModel> = emptyList(),
    val audioTracks: List<PlaybackTrackModel> = emptyList(),
    val textTracks: List<PlaybackTrackModel> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val selectedTextTrackId: String? = null,
    val speed: Float = 1f,
    val resizeMode: PlaybackResizeMode = PlaybackResizeMode.Fit,
    val settingsPage: PlayerSettingsPage? = null,
    val isPipSupported: Boolean = false,
    val isInPip: Boolean = false,
    val seekFeedbackSeconds: Int? = null,
    val error: PlaybackErrorModel? = null,
) {
    val canSeek: Boolean get() = durationMillis > 0L
    val isBuffering: Boolean get() = phase == PlaybackPhase.Buffering
    val isEnded: Boolean get() = phase == PlaybackPhase.Ended
}
