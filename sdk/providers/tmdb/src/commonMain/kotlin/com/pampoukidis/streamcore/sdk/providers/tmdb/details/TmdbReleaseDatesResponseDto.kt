package com.pampoukidis.streamcore.sdk.providers.tmdb.details

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbReleaseDatesResponseDto(
    val results: List<TmdbReleaseDatesCountryDto> = emptyList(),
)