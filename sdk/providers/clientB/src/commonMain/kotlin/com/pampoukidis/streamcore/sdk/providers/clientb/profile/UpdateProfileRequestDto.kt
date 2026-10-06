package com.pampoukidis.streamcore.sdk.providers.clientb.profile

internal data class UpdateProfileRequestDto(
    val profileId: String,
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
