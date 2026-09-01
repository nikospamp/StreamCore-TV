package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbReleaseDateDto(
    val certification: String = "",
    val type: Int = 0,
)