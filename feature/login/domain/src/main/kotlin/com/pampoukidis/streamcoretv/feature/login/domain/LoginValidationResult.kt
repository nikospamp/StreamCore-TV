package com.pampoukidis.streamcoretv.feature.login.domain

data class LoginValidationResult(
    val emailError: LoginFieldError?,
    val passwordError: LoginFieldError?,
) {
    val isValid: Boolean = emailError == null && passwordError == null
}
