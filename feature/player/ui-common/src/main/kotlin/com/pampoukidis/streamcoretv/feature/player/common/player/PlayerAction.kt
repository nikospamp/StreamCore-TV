package com.pampoukidis.streamcoretv.feature.player.common.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode

sealed interface PlayerAction {
    data class Load(val request: PlaybackRequestModel, val isPipSupported: Boolean) : PlayerAction
    data object BackSelected : PlayerAction
    data object ToggleControls : PlayerAction
    data object TogglePlayPause : PlayerAction
    data class SeekBy(val deltaMillis: Long, val showFeedback: Boolean = false) : PlayerAction
    data object ScrubStarted : PlayerAction
    data class ScrubChanged(val positionMillis: Long) : PlayerAction
    data object ScrubFinished : PlayerAction
    data class OpenSettings(val page: PlayerSettingsPage = PlayerSettingsPage.Root) : PlayerAction
    data object CloseSettings : PlayerAction
    data class SelectVideoTrack(val trackId: String?) : PlayerAction
    data class SelectAudioTrack(val trackId: String?) : PlayerAction
    data class SelectTextTrack(val trackId: String?) : PlayerAction
    data class SelectSpeed(val speed: Float) : PlayerAction
    data class SelectResizeMode(val mode: PlaybackResizeMode) : PlayerAction
    data object Retry : PlayerAction
    data object PipSelected : PlayerAction
    data class PipChanged(val isInPip: Boolean) : PlayerAction
    data class ForegroundChanged(val isForeground: Boolean) : PlayerAction
}