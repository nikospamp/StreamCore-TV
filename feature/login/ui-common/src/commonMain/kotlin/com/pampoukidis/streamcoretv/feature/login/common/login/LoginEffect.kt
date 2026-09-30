package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface LoginEffect {
    data object LoginSucceeded : LoginEffect
    data object ForgotPassword : LoginEffect
    data object CreateAccount : LoginEffect
    data object Help : LoginEffect
    data class ShowError(val error: StreamCoreError) : LoginEffect
}
