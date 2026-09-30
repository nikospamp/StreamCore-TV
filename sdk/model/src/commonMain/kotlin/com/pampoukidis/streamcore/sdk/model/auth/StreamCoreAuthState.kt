package com.pampoukidis.streamcore.sdk.model.auth

sealed interface StreamCoreAuthState {
    data object LoggedOut : StreamCoreAuthState

    data class LoggedIn(
        val account: StreamCoreAuthAccount,
    ) : StreamCoreAuthState
}
