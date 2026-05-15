package com.pampoukidis.streamcoretv.feature.profiles.common.contract

sealed interface ProfilesAction {
    data object Refresh : ProfilesAction
    data class SelectProfile(val profileId: String) : ProfilesAction
    data class RequestDeleteProfile(val profileId: String) : ProfilesAction
    data object ConfirmDeleteProfile : ProfilesAction
    data object DismissDeleteConfirmation : ProfilesAction
}
