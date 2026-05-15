package com.pampoukidis.streamcoretv.data.client.model

internal data class CreateProfileRequestDto(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
