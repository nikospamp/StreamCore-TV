package com.pampoukidis.streamcoretv.data.tmdb.model

internal data class CreateProfileRequestDto(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
