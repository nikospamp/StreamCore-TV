package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

sealed interface ProfilesAction {
    data object RouteEntered : ProfilesAction
    data object Refresh : ProfilesAction
    data object ManageProfiles : ProfilesAction
    data object DoneManaging : ProfilesAction
    data class SelectProfile(val profileId: String) : ProfilesAction
    data class RequestDeleteProfile(val profileId: String) : ProfilesAction
    data object ConfirmDeleteProfile : ProfilesAction
    data object DismissDeleteConfirmation : ProfilesAction
    data class PinDraftChanged(val value: String) : ProfilesAction {
        override fun toString(): String {
            return "PinDraftChanged(value=<redacted>)"
        }
    }
    data class PinDigitEntered(val digit: Int) : ProfilesAction {
        override fun toString(): String {
            return "PinDigitEntered(digit=<redacted>)"
        }
    }
    data object PinDeleteDigit : ProfilesAction
    data object RetryPin : ProfilesAction
    data object CancelPin : ProfilesAction
}

