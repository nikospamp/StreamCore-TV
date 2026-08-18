package com.pampoukidis.streamcoretv.playback.api

data class PlaybackMediaModel(
    val assetId: String,
    val title: String,
    val uri: String? = null,
    val mimeType: String? = null,
)