package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbVideosResponseDto(
    val results: List<TmdbVideoDto> = emptyList(),
)
