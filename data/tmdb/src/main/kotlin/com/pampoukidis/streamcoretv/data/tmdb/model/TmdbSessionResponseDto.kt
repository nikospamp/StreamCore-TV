package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbSessionResponseDto(
    val success: Boolean = false,
    @SerialName("session_id")
    val sessionId: String,
)