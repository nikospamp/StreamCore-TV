package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbMovieAccountStatesDto(
    val id: Int,
    val favorite: Boolean = false,
    val watchlist: Boolean = false,
)