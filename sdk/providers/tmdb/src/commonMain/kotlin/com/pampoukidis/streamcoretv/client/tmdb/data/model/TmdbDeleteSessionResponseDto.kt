package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbDeleteSessionResponseDto(
    val success: Boolean = false,
)