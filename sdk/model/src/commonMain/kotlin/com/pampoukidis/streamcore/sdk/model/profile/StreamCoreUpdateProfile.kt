package com.pampoukidis.streamcore.sdk.model.profile

data class StreamCoreUpdateProfile(
    val profileId: String,
    val displayName: String,
    val avatarId: String,
    val parentalLevelId: String,
)
