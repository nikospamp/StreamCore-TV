package com.pampoukidis.streamcoretv.core.model.auth

data class UpdateProfileModel(
    val profileId: String,
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)