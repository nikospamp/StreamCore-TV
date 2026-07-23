package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbRequestTokenResponseDto(
    val success: Boolean = false,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("request_token")
    val requestToken: String,
)