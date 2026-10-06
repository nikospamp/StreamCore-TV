package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbMovieAccountStatesDto(
    val id: Int,
    val favorite: Boolean = false,
    val watchlist: Boolean = false,
)