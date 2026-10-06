package com.pampoukidis.streamcore.sdk.runtime.session

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile

/** One granted profile authorization; an equal profile ID alone cannot reuse this grant. */
internal class AuthorizedProfile(
    val id: String,
    val profileSelection: ProfileSelectionAttempt,
    val profile: StreamCoreProfile,
    val pinVerification: ProfilePinVerification?,
) {
    val accountSession: AccountSession
        get() { return profileSelection.accountSession }
    val providers: ProviderSessionServices
        get() { return accountSession.providers }
}
