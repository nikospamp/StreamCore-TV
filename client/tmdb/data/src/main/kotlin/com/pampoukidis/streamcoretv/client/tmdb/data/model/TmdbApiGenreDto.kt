package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbApiGenreDto(
    val id: Int,
    val name: String,
)