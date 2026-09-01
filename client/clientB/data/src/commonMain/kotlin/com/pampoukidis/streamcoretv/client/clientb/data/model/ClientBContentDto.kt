package com.pampoukidis.streamcoretv.client.clientb.data.model

internal data class ClientBContentDto(
    val assetId: String,
    val displayTitle: String,
    val description: String,
    val ratingOutOfTen: Int,
    val parentalRating: String,
    val parentalLevel: Int,
    val portraitImage: String,
    val landscapeImage: String?,
    val releaseEpochMillis: Long,
    val contributors: List<ClientBContributorDto>,
    val categories: List<ClientBCategoryDto>,
)
