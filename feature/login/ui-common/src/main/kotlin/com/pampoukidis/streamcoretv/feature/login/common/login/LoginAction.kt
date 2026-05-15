package com.pampoukidis.streamcoretv.feature.login.common.login

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
    data object ForgotPassword : LoginAction
    data object CreateAccount : LoginAction
    data object Help : LoginAction
}
