package com.pampoukidis.streamcoretv.feature.profiles.common.editor

data class ProfileDraftModel(
    val profileId: String? = null,
    val displayName: String = "",
    val avatarId: String = "",
    val parentalLevelId: String = "",
)
