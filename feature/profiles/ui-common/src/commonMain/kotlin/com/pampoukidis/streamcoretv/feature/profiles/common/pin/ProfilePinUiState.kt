package com.pampoukidis.streamcoretv.feature.profiles.common.pin

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile

/** A memory-only input draft. Never put this state in saved state or navigation arguments. */
data class ProfilePinUiState(
    val profile: StreamCoreProfile,
    val digitCount: Int,
    val draft: String = "",
    val isSubmitting: Boolean = false,
    val failure: ProfilePinFailure? = null,
    val inputRevision: Int = 0,
    val challengeId: String = "",
) {
    val canRetry: Boolean
        get() = failure == ProfilePinFailure.Unavailable && draft.length == digitCount && !isSubmitting

    val inputEnabled: Boolean
        get() = !isSubmitting && failure != ProfilePinFailure.Locked

    override fun toString(): String {
        return "ProfilePinUiState(profileId=" + profile.id + ", digitCount=" + digitCount +
            ", draft=<redacted>, isSubmitting=" + isSubmitting + ", failure=" + failure + ")"
    }
}
