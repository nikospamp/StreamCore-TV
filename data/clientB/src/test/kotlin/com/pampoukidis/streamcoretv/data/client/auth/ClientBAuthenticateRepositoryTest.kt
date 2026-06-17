package com.pampoukidis.streamcoretv.data.client.auth

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientBAuthenticateRepositoryTest {

    private val subject = ClientBAuthenticateRepository()

    @Test
    fun `auth operations complete`() = runTest {
        assertEquals(
            AppResult.Success(Unit),
            subject.loginUser(
                identifier = "lead@streamcore.tv",
                password = "password",
            ),
        )
        assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
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
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
    }
}
