package com.pampoukidis.streamcore.sdk.providers.tmdb.profile

internal data class CreateProfileRequestDto(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
