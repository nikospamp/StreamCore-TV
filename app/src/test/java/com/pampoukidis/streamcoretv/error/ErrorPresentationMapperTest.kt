package com.pampoukidis.streamcoretv.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.error.DefaultErrorPresentationPresentationMapper
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorPresentationMapperTest {

    private val subject = DefaultErrorPresentationPresentationMapper()

    @Test
    fun `authentication error maps to sign-in presentation`() {
        val result = subject.map(AppError.Authentication())

        assertEquals(Res.string.error_authentication_title, result.title)
        assertEquals(Res.string.error_authentication_message, result.message)
        assertTrue(result.dismissible)
    }

    @Test
    fun `network error maps to connection presentation`() {
        val result = subject.map(AppError.Network())

        assertEquals(Res.string.error_network_title, result.title)
        assertEquals(Res.string.error_network_message, result.message)
        assertTrue(result.dismissible)
    }

    @Test
    fun `server and unknown errors map to generic presentation`() {
        val serverResult = subject.map(AppError.Server())
        val unknownResult = subject.map(AppError.Unknown())

        assertEquals(Res.string.error_generic_title, serverResult.title)
        assertEquals(Res.string.error_generic_message, serverResult.message)
        assertEquals(Res.string.error_generic_title, unknownResult.title)
        assertEquals(Res.string.error_generic_message, unknownResult.message)
    }

    @Test
    fun `session expired is not dismissible`() {
        val result = subject.map(AppError.SessionExpired())

        assertEquals(Res.string.error_session_expired_title, result.title)
        assertFalse(result.dismissible)
    }
}
