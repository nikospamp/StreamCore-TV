package com.pampoukidis.streamcore.sdk.model.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LoginCredentialsTest {
    @Test
    fun diagnosticRepresentationDoesNotRevealEitherCredentialField() {
        val credentials = StreamCoreLoginCredentials("private-person@example.test", "private-password-9081")
        val diagnostic = credentials.toString()
        assertFalse(diagnostic.contains(credentials.identifier))
        assertFalse(diagnostic.contains(credentials.password))
        assertEquals("private-person@example.test", credentials.identifier)
        assertEquals("private-password-9081", credentials.password)
    }
}
