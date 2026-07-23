package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbAccountDetailsDto(
    val id: Int,
    val username: String,
    @SerialName("name")
    val displayName: String? = null,
)