package com.pampoukidis.streamcore.sdk.model.playback

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import kotlinx.serialization.Serializable

@Serializable
data class StreamCorePlaybackRequest(
    val profileId: String,
    val contentId: String,
    val contentSnapshot: StreamCoreContent,
)