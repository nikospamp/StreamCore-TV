package com.pampoukidis.streamcoretv.feature.login.common.contract

import com.pampoukidis.streamcoretv.feature.login.domain.LoginCredentials

sealed interface LoginEvent {
    data class SubmitCredentials(val credentials: LoginCredentials) : LoginEvent
    data object ForgotPassword : LoginEvent
    data object CreateAccount : LoginEvent
    data object Help : LoginEvent
}
