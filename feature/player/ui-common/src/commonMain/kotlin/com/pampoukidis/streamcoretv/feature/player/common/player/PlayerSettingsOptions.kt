package com.pampoukidis.streamcoretv.feature.player.common.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode

val PlayerSettingsSpeedOptions: List<Pair<String, Float>> = listOf(
    "0.5×" to 0.5f,
    "0.75×" to 0.75f,
    "1.0×" to 1f,
    "1.25×" to 1.25f,
    "1.5×" to 1.5f,
    "2.0×" to 2f,
)

val PlayerSettingsResizeModeOptions: List<Pair<String, PlaybackResizeMode>> = PlaybackResizeMode.entries.map { mode ->
    mode.name to mode
}
