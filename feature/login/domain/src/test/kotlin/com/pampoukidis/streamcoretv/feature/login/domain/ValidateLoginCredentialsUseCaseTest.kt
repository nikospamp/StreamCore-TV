package com.pampoukidis.streamcoretv.feature.login.domain

import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateLoginCredentialsUseCaseTest {

    private val subject = ValidateLoginCredentialsUseCase()

    @Test
    fun `valid credentials pass validation`() {
        val result = subject(
            email = "lead@streamcore.tv",
            password = "password",
        )

        assertTrue(result.isValid)
        assertNull(result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun `blank credentials return required errors`() {
        val result = subject(
            email = "",
            password = "",
        )

        assertEquals(LoginFieldError.Required, result.emailError)
        assertEquals(LoginFieldError.Required, result.passwordError)
    }

    @Test
    fun `invalid email returns email error`() {
        val result = subject(
            email = "lead",
            password = "password",
        )

        assertEquals(LoginFieldError.InvalidEmail, result.emailError)
        assertNull(result.passwordError)
    }
}
