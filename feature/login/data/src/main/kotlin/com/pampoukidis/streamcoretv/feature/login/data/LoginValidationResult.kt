package com.pampoukidis.streamcoretv.feature.login.data

data class LoginValidationResult(
    val emailError: LoginFieldError?,
    val passwordError: LoginFieldError?,
) {
    val isValid: Boolean = emailError == null && passwordError == null
}
