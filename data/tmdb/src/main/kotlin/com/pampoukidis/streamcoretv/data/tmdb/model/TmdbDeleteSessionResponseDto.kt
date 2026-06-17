package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbDeleteSessionResponseDto(
    val success: Boolean = false,
)