package com.pampoukidis.streamcore.sdk.api.validation

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginFieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginValidatorTest {

    private val subject = LoginValidator

    @Test
    fun `valid credentials pass validation`() {
        val result = subject.validate(
            identifier = "lead@streamcore.tv",
            password = "password",
        )

        assertTrue(result.isValid)
        assertNull(result.identifierError)
        assertNull(result.passwordError)
    }

    @Test
    fun `blank credentials return required errors`() {
        val result = subject.validate(
            identifier = "",
            password = "",
        )

        assertEquals(StreamCoreLoginFieldError.Required, result.identifierError)
        assertEquals(StreamCoreLoginFieldError.Required, result.passwordError)
    }

    @Test
    fun `whitespace credentials are normalized to blank for validation`() {
        val result = subject.validate(
            identifier = "  \t ",
            password = "  \n ",
        )

        assertEquals(StreamCoreLoginFieldError.Required, result.identifierError)
        assertEquals(StreamCoreLoginFieldError.Required, result.passwordError)
    }

    @Test
    fun `non email identifier passes validation`() {
        val result = subject.validate(
            identifier = "tmdb_user",
            password = "password",
        )

        assertTrue(result.isValid)
        assertNull(result.identifierError)
        assertNull(result.passwordError)
    }
}
