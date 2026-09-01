package com.pampoukidis.streamcoretv.feature.login.domain

import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidateLoginCredentialsUseCaseTest {

    private val subject = ValidateLoginCredentialsUseCase()

    @Test
    fun `valid credentials pass validation`() {
        val result = subject(
            identifier = "lead@streamcore.tv",
            password = "password",
        )

        assertTrue(result.isValid)
        assertNull(result.identifierError)
        assertNull(result.passwordError)
    }

    @Test
    fun `blank credentials return required errors`() {
        val result = subject(
            identifier = "",
            password = "",
        )

        assertEquals(LoginFieldError.Required, result.identifierError)
        assertEquals(LoginFieldError.Required, result.passwordError)
    }

    @Test
    fun `whitespace credentials are normalized to blank for validation`() {
        val result = subject(
            identifier = "  \t ",
            password = "  \n ",
        )

        assertEquals(LoginFieldError.Required, result.identifierError)
        assertEquals(LoginFieldError.Required, result.passwordError)
    }

    @Test
    fun `non email identifier passes validation`() {
        val result = subject(
            identifier = "tmdb_user",
            password = "password",
        )

        assertTrue(result.isValid)
        assertNull(result.identifierError)
        assertNull(result.passwordError)
    }
}
