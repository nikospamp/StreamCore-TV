package com.pampoukidis.streamcoretv.data.clienta.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientAAuthenticateRepositoryTest {

    private val subject = ClientAAuthenticateRepository()

    @Test
    fun `login succeeds after delay with mock credentials`() = runTest {
        subject.loginUser(
            email = "nikos@mail.com",
            password = "123456",
        )

        assertEquals(4_000L, currentTime)
    }

    @Test
    fun `login fails after delay with invalid credentials`() = runTest {
        var failure: IllegalArgumentException? = null

        try {
            subject.loginUser(
                email = "lead@streamcore.tv",
                password = "password",
            )
            fail("Expected invalid credentials to fail")
        } catch (exception: IllegalArgumentException) {
            failure = exception
        }

        assertEquals(4_000L, currentTime)
        assertTrue(failure?.message.orEmpty().contains("Invalid email or password"))
    }

    @Test
    fun `secondary auth operations complete`() = runTest {
        subject.loginUserWithQR(qrCode = "qr-code")
        subject.forgotPassword(
            email = "lead@streamcore.tv",
            otp = null,
        )
        subject.logoutUser()
    }
}
