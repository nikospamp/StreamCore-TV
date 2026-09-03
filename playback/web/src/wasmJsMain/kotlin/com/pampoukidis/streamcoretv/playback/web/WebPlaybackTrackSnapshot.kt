package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType

internal data class WebPlaybackTrackSnapshot(
    val id: String,
    val type: PlaybackTrackType,
    val label: String,
    val language: String? = null,
    val videoHeight: Int? = null,
    val isSupported: Boolean = true,
)
