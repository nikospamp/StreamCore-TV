package com.pampoukidis.streamcoretv.feature.login.domain

import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError
import com.pampoukidis.streamcoretv.feature.login.data.LoginValidationResult
import javax.inject.Inject

class ValidateLoginCredentialsUseCase @Inject constructor() {
    operator fun invoke(
        identifier: String,
        password: String,
    ): LoginValidationResult {
        val normalizedIdentifier = identifier.trim()

        return LoginValidationResult(
            identifierError = when {
                normalizedIdentifier.isBlank() -> LoginFieldError.Required
                else -> null
            },
            passwordError = when {
                password.isBlank() -> LoginFieldError.Required
                else -> null
            },
        )
    }
}
