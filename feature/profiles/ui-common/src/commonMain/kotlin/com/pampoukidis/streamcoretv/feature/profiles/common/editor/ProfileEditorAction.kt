package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode

sealed interface ProfileEditorAction {
    data class Load(val mode: ProfileEditorMode, val profileId: String?) : ProfileEditorAction
    data class DisplayNameChanged(val value: String) : ProfileEditorAction
    data class AvatarChanged(val avatarId: String) : ProfileEditorAction
    data class ParentalLevelChanged(val parentalLevelId: String) : ProfileEditorAction
    data class KidsProfileChanged(val isKids: Boolean) : ProfileEditorAction
    data object RequestDeleteProfile : ProfileEditorAction
    data object ConfirmDeleteProfile : ProfileEditorAction
    data object DismissDeleteConfirmation : ProfileEditorAction
    data object Submit : ProfileEditorAction
    data object Cancel : ProfileEditorAction
}