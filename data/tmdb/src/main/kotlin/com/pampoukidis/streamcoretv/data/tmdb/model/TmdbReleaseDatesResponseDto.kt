package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbReleaseDatesResponseDto(
    val results: List<TmdbReleaseDatesCountryDto> = emptyList(),
)