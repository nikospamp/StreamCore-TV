package com.pampoukidis.streamcoretv.core.model.auth

data class ProfileModel(
    val id: String,
    val displayName: String,
    val avatar: ProfileAvatarModel,
    val parentalLevel: ProfileParentalLevelModel,
    val canDelete: Boolean,
    val isKidsProfile: Boolean,
)
