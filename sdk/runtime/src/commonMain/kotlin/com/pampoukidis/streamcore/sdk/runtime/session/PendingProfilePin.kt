package com.pampoukidis.streamcore.sdk.runtime.session

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfilePinChallenge

/** A PIN request belonging to one profile-selection attempt. */
internal class PendingProfilePin(
    val profileSelection: ProfileSelectionAttempt,
    val model: StreamCoreProfilePinChallenge
) {
    val accountSession: AccountSession
        get() {
            return profileSelection.accountSession
        }
}
