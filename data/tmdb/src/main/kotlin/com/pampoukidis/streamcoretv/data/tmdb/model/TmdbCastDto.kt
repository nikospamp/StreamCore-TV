package com.pampoukidis.streamcoretv.data.tmdb.model

internal data class TmdbCastDto(
    val personId: String,
    val fullName: String,
    val character: String?,
    val portraitUrl: String?,
)
