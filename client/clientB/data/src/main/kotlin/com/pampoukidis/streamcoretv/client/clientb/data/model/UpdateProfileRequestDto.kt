package com.pampoukidis.streamcoretv.client.clientb.data.model

internal data class UpdateProfileRequestDto(
    val profileId: String,
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
