package com.pampoukidis.streamcoretv.core.model.auth

data class ProfileEditorOptionsModel(
    val avatars: List<ProfileAvatarModel>,
    val parentalLevels: List<ProfileParentalLevelModel>,
)
