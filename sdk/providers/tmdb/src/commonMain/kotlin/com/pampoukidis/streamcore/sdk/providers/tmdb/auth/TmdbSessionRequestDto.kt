package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbSessionRequestDto(
    @SerialName("request_token")
    val requestToken: String,
)