package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbSessionRequestDto(
    @SerialName("request_token")
    val requestToken: String,
)