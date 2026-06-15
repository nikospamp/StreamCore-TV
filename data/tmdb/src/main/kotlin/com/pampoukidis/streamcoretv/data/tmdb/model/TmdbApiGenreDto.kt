package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbApiGenreDto(
    val id: Int,
    val name: String,
)