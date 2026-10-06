package com.pampoukidis.streamcore.sdk.runtime.session


/** Atomic lifecycle/account/profile state. Closed refers to the SDK instance, not logout or a screen. */
internal data class ClientSessionState(
    val accountSession: AccountSession? = null,
    val authorizedProfile: AuthorizedProfile? = null,
    val profileSelection: ProfileSelectionAttempt? = null,
    val pinChallenge: PendingProfilePin? = null,
    val pinVerification: ProfilePinVerification? = null,
    val selectionVersion: Long = 0,
    val isAuthInitialized: Boolean = false,
    val isClientClosed: Boolean = false,
) {
    fun withoutProfileAuthorization(): ClientSessionState {
        return copy(
            authorizedProfile = null,
            profileSelection = null,
            pinChallenge = null,
            pinVerification = null,
            selectionVersion = selectionVersion + 1
        )
    }
}
