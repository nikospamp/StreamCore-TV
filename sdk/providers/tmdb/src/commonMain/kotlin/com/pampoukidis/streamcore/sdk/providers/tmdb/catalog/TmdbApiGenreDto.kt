package com.pampoukidis.streamcore.sdk.providers.tmdb.catalog

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbApiGenreDto(
    val id: Int,
    val name: String,
)