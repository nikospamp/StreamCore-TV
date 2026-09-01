package com.pampoukidis.streamcoretv.core.model.auth

data class CreateProfileModel(
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
