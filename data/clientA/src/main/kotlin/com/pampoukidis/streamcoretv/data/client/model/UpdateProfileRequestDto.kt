package com.pampoukidis.streamcoretv.data.client.model

internal data class UpdateProfileRequestDto(
    val profileId: String,
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
