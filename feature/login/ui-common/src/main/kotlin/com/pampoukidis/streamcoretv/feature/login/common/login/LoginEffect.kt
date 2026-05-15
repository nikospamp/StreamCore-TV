package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface LoginEffect {
    data object LoginSucceeded : LoginEffect
    data object ForgotPassword : LoginEffect
    data object CreateAccount : LoginEffect
    data object Help : LoginEffect
    data class ShowError(val error: AppError) : LoginEffect
}
