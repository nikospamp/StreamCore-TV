package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbGenreListResponseDto(
    val genres: List<TmdbApiGenreDto>,
)