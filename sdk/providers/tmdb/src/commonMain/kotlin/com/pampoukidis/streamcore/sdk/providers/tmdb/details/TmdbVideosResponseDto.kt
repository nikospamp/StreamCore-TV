package com.pampoukidis.streamcore.sdk.providers.tmdb.details

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbVideosResponseDto(
    val results: List<TmdbVideoDto> = emptyList(),
)
