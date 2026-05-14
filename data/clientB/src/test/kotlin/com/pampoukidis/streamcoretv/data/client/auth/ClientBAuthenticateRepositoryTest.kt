package com.pampoukidis.streamcoretv.data.client.auth

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientBAuthenticateRepositoryTest {

    private val subject = ClientBAuthenticateRepository()

    @Test
    fun `auth operations complete`() = runTest {
        assertEquals(
            AppResult.Success(Unit),
            subject.loginUser(
                email = "lead@streamcore.tv",
                password = "password",
            ),
        )
        assertEquals(
            AppResult.Success(Unit),
            subject.loginUserWithQR(qrCode = "qr-code"),
        )
        assertEquals(
            AppResult.Success(Unit),
            subject.forgotPassword(
                email = "lead@streamcore.tv",
                otp = null,
            ),
        )
        assertEquals(AppResult.Success(Unit), subject.logoutUser())
    }
}
