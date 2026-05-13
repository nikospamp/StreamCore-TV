package com.pampoukidis.streamcoretv.feature.login.domain

class ValidateLoginCredentialsUseCase {
    operator fun invoke(
        email: String,
        password: String,
    ): LoginValidationResult {
        val normalizedEmail = email.trim()

        return LoginValidationResult(
            emailError = when {
                normalizedEmail.isBlank() -> LoginFieldError.Required
                !EmailRegex.matches(normalizedEmail) -> LoginFieldError.InvalidEmail
                else -> null
            },
            passwordError = when {
                password.isBlank() -> LoginFieldError.Required
                else -> null
            },
        )
    }

    private companion object {
        val EmailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}
