package com.pampoukidis.streamcoretv.playback.api

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackProgressEntryModel(
    val profileId: String,
    val contentId: String,
    val contentSnapshot: ContentModel,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
)