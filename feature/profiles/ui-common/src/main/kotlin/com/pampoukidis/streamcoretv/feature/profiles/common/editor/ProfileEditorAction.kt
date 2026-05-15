package com.pampoukidis.streamcoretv.feature.profiles.common.editor

sealed interface ProfileEditorAction {
    data class Load(
        val mode: ProfileEditorMode,
        val profileId: String?,
    ) : ProfileEditorAction

    data class DisplayNameChanged(val value: String) : ProfileEditorAction
    data class AvatarChanged(val avatarId: String) : ProfileEditorAction
    data class ParentalLevelChanged(val parentalLevelId: String) : ProfileEditorAction
    data object Submit : ProfileEditorAction
    data object Cancel : ProfileEditorAction
}


