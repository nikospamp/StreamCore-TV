package com.pampoukidis.streamcore.sdk.providers.tmdb.catalog

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbGenreListResponseDto(
    val genres: List<TmdbApiGenreDto>,
)