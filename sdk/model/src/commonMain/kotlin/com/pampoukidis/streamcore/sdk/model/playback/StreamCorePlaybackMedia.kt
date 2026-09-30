package com.pampoukidis.streamcore.sdk.model.playback

data class StreamCorePlaybackMedia(
    val assetId: String,
    val title: String,
    val uri: String? = null,
    val mimeType: String? = null,
)