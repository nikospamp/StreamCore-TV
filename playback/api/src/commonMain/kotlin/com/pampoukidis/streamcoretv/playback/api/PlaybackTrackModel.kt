package com.pampoukidis.streamcoretv.playback.api

data class PlaybackTrackModel(
    val id: String,
    val type: PlaybackTrackType,
    val label: String,
    val language: String? = null,
    val videoHeight: Int? = null,
    val isSupported: Boolean = true,
)