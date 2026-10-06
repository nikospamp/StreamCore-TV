package com.pampoukidis.streamcore.sdk.providers.clientb.profile

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
    val pinProtected: Boolean = false,
)
