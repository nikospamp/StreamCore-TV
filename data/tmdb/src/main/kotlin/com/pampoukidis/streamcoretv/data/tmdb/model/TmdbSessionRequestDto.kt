package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbSessionRequestDto(
    @SerialName("request_token")
    val requestToken: String,
)