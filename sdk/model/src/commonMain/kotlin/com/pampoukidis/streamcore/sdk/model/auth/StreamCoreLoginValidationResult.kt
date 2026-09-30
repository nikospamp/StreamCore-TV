package com.pampoukidis.streamcore.sdk.model.auth

data class StreamCoreLoginValidationResult(
    val identifierError: StreamCoreLoginFieldError?,
    val passwordError: StreamCoreLoginFieldError?,
) {
    val isValid: Boolean = identifierError == null && passwordError == null
}
