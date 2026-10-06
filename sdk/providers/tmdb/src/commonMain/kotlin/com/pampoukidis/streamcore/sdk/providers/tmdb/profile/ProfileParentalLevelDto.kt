package com.pampoukidis.streamcore.sdk.providers.tmdb.profile

internal data class ProfileParentalLevelDto(
    val id: String,
    val label: String,
    val rank: Int,
    val isKids: Boolean,
)
