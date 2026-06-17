package com.pampoukidis.streamcoretv.core.model.auth

sealed interface AuthStateModel {
    data object LoggedOut : AuthStateModel

    data class LoggedIn(
        val account: AuthAccountModel?,
    ) : AuthStateModel
}