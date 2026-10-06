package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbValidateLoginRequestDto(
    val username: String,
    val password: String,
    @SerialName("request_token")
    val requestToken: String,
)