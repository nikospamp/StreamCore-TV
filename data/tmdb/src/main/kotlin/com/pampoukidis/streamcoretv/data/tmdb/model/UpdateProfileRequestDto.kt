package com.pampoukidis.streamcoretv.data.tmdb.model

internal data class UpdateProfileRequestDto(
    val profileId: String,
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
