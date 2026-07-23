package com.pampoukidis.streamcoretv.client.tmdb.data.model

internal data class ProfileDto(
    val id: String,
    val displayName: String,
    val avatarId: String,
    val avatarUrl: String?,
    val parentalLevelId: String,
    val parentalLevelLabel: String,
    val parentalLevelRank: Int,
    val canDelete: Boolean,
    val isKidsProfile: Boolean,
)
