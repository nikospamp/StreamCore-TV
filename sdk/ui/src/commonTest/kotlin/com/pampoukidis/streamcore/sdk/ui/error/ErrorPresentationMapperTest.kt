package com.pampoukidis.streamcore.sdk.ui.error

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationIssue
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.ui.error.DefaultErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.ui.generated.resources.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ErrorPresentationMapperTest {

    private val subject = DefaultErrorPresentationMapper()

    @Test
    fun `profile PIN rejection has profile wording rather than account sign-in wording`() {
        val result = subject.map(StreamCoreError.PinRejected())
        assertEquals(Res.string.profile_pin_title, result.title)
        assertEquals(Res.string.profile_pin_incorrect, result.message)
    }

    @Test
    fun `authentication error maps to sign-in presentation`() {
        val result = subject.map(StreamCoreError.Authentication())

        assertEquals(Res.string.error_authentication_title, result.title)
        assertEquals(Res.string.error_authentication_message, result.message)
        assertTrue(result.dismissible)
    }

    @Test
    fun `network error maps to connection presentation`() {
        val result = subject.map(StreamCoreError.Network())

        assertEquals(Res.string.error_network_title, result.title)
        assertEquals(Res.string.error_network_message, result.message)
        assertTrue(result.dismissible)
    }

    @Test
    fun `server and unknown errors map to generic presentation`() {
        val serverResult = subject.map(StreamCoreError.Server())
        val unknownResult = subject.map(StreamCoreError.Unknown())

        assertEquals(Res.string.error_generic_title, serverResult.title)
        assertEquals(Res.string.error_generic_message, serverResult.message)
        assertEquals(Res.string.error_generic_title, unknownResult.title)
        assertEquals(Res.string.error_generic_message, unknownResult.message)
    }

    @Test
    fun `session expired is not dismissible`() {
        val result = subject.map(StreamCoreError.SessionExpired())

        assertEquals(Res.string.error_session_expired_title, result.title)
        assertEquals(Res.string.error_session_expired_message, result.message)
        assertEquals(Res.string.error_action_ok, result.confirmAction)
        assertFalse(result.dismissible)
    }

    @Test
    fun `timeout and authorization failures retain distinct recovery copy`() {
        val timeout = subject.map(StreamCoreError.Timeout())
        val unauthorized = subject.map(StreamCoreError.Unauthorized())

        assertEquals(Res.string.error_timeout_title, timeout.title)
        assertEquals(Res.string.error_timeout_message, timeout.message)
        assertEquals(Res.string.error_unauthorized_title, unauthorized.title)
        assertEquals(Res.string.error_unauthorized_message, unauthorized.message)
        assertTrue(timeout.dismissible)
        assertTrue(unauthorized.dismissible)
    }

    @Test
    fun `SDK validation capability and infrastructure failures have safe generic copy`() {
        val errors = listOf(
            StreamCoreError.Parsing(),
            StreamCoreError.Unsupported("playback.sources"),
            StreamCoreError.Validation(listOf(StreamCoreValidationIssue(StreamCoreValidationField.Password, StreamCoreValidationReason.Required))),
            StreamCoreError.InvalidContext(StreamCoreContextFailureReason.Unauthenticated),
            StreamCoreError.Storage(),
            StreamCoreError.Closed(),
        )
        for (error in errors) {
            val result = subject.map(error)
            assertEquals(Res.string.error_generic_title, result.title)
            assertEquals(Res.string.error_generic_message, result.message)
            assertEquals(Res.string.error_action_ok, result.confirmAction)
            assertTrue(result.dismissible)
        }
    }

    @Test
    fun `backend diagnostic text never replaces user-facing resources`() {
        val diagnostic = StreamCoreErrorSource(
            backendCode = "provider-internal-code",
            backendMessage = "Untrusted transport diagnostic content",
            requestId = "internal-request-id",
        )

        assertEquals(
            subject.map(StreamCoreError.Authentication()),
            subject.map(StreamCoreError.Authentication(remainingAttempts = 2, source = diagnostic)),
        )
        assertEquals(subject.map(StreamCoreError.Unknown()), subject.map(StreamCoreError.Unknown(source = diagnostic)))
    }
}
