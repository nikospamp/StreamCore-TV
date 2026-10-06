package com.pampoukidis.streamcore.sdk.providers.clientb.profile

internal data class CreateProfileRequestDto(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
