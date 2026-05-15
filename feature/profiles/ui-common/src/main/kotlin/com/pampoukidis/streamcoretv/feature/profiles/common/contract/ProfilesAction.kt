package com.pampoukidis.streamcoretv.feature.profiles.common.contract

sealed interface ProfilesAction {
    data object Refresh : ProfilesAction
    data class SelectProfile(val profileId: String) : ProfilesAction
    data object StartCreateProfile : ProfilesAction
    data class StartEditProfile(val profileId: String) : ProfilesAction
    data class DisplayNameChanged(val value: String) : ProfilesAction
    data class AvatarChanged(val avatarId: String) : ProfilesAction
    data class ParentalLevelChanged(val parentalLevelId: String) : ProfilesAction
    data object SubmitEditor : ProfilesAction
    data object DismissEditor : ProfilesAction
    data class RequestDeleteProfile(val profileId: String) : ProfilesAction
    data object ConfirmDeleteProfile : ProfilesAction
    data object DismissDeleteConfirmation : ProfilesAction
}
