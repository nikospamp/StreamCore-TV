package com.pampoukidis.streamcore.sdk.providers.tmdb.details

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbReleaseDateDto(
    val certification: String = "",
    val type: Int = 0,
)