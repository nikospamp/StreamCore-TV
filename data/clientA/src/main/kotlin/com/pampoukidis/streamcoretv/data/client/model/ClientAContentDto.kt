package com.pampoukidis.streamcoretv.data.client.model

internal data class ClientAContentDto(
    val contentId: String,
    val name: String,
    val synopsis: String,
    val score: Int,
    val maturityLabel: String,
    val maturityRank: Int,
    val posterUrl: String,
    val heroImageUrl: String?,
    val publishedAtEpochMs: Long,
    val cast: List<ClientACastDto>,
    val genres: List<ClientAGenreDto>,
)
