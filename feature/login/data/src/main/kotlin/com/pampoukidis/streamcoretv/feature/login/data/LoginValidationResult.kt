package com.pampoukidis.streamcoretv.feature.login.data

data class LoginValidationResult(
    val identifierError: LoginFieldError?,
    val passwordError: LoginFieldError?,
) {
    val isValid: Boolean = identifierError == null && passwordError == null
}
