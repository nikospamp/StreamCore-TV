package com.pampoukidis.streamcoretv.client.tmdb.data.model

internal data class CreateProfileRequestDto(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
