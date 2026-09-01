package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbValidateLoginRequestDto(
    val username: String,
    val password: String,
    @SerialName("request_token")
    val requestToken: String,
)