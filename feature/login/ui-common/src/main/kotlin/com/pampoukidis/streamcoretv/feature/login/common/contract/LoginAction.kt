package com.pampoukidis.streamcoretv.feature.login.common.contract

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
    data object ForgotPassword : LoginAction
    data object CreateAccount : LoginAction
    data object Help : LoginAction
    data object DismissError : LoginAction
}
