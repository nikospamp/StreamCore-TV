package com.pampoukidis.streamcoretv.client.clientb.data.model

internal data class CreateProfileRequestDto(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
