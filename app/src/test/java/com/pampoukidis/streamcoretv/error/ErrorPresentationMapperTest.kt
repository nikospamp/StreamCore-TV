package com.pampoukidis.streamcoretv.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.error.DefaultErrorPresentationPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorPresentationMapperTest {

    private val subject = DefaultErrorPresentationPresentationMapper()

    @Test
    fun `authentication error maps to sign-in presentation`() {
        val result = subject.map(AppError.Authentication())

        assertEquals(R.string.error_authentication_title, result.titleRes)
        assertEquals(R.string.error_authentication_message, result.messageRes)
        assertTrue(result.dismissible)
    }

    @Test
    fun `network error maps to connection presentation`() {
        val result = subject.map(AppError.Network())

        assertEquals(R.string.error_network_title, result.titleRes)
        assertEquals(R.string.error_network_message, result.messageRes)
        assertTrue(result.dismissible)
    }

    @Test
    fun `server and unknown errors map to generic presentation`() {
        val serverResult = subject.map(AppError.Server())
        val unknownResult = subject.map(AppError.Unknown())

        assertEquals(R.string.error_generic_title, serverResult.titleRes)
        assertEquals(R.string.error_generic_message, serverResult.messageRes)
        assertEquals(R.string.error_generic_title, unknownResult.titleRes)
        assertEquals(R.string.error_generic_message, unknownResult.messageRes)
    }

    @Test
    fun `session expired is not dismissible`() {
        val result = subject.map(AppError.SessionExpired())

        assertEquals(R.string.error_session_expired_title, result.titleRes)
        assertFalse(result.dismissible)
    }
}
