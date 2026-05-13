package com.pampoukidis.streamcoretv.feature.login.common.contract

sealed interface LoginEvent {
    data object LoginSucceeded : LoginEvent
    data object ForgotPassword : LoginEvent
    data object CreateAccount : LoginEvent
    data object Help : LoginEvent
}
