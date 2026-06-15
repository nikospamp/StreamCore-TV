package com.pampoukidis.streamcoretv.data.tmdb.model

internal data class TmdbContentDto(
    val contentId: String,
    val name: String,
    val synopsis: String,
    val score: Int,
    val maturityLabel: String,
    val maturityRank: Int,
    val posterUrl: String,
    val heroImageUrl: String?,
    val publishedAtEpochMs: Long,
    val cast: List<TmdbCastDto>,
    val genres: List<TmdbGenreDto>,
)
