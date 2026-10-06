package com.pampoukidis.streamcore.sdk.validation

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginFieldError
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginValidationResult

/** Backend-free form feedback. Authentication operations repeat authoritative validation before calling the provider. */
object LoginValidator {
    /** Checks a trimmed identifier and nonblank password without modifying password contents. */
    fun validate(
        identifier: String,
        password: String,
    ): StreamCoreLoginValidationResult {
        val normalizedIdentifier = identifier.trim()

        return StreamCoreLoginValidationResult(
            identifierError = when {
                normalizedIdentifier.isBlank() -> StreamCoreLoginFieldError.Required
                else -> null
            },
            passwordError = when {
                password.isBlank() -> StreamCoreLoginFieldError.Required
                else -> null
            },
        )
    }
}
