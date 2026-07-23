package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbDeleteSessionRequestDto(
    @SerialName("session_id")
    val sessionId: String,
)