package com.pampoukidis.streamcoretv.data.clientb.auth

import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClientBAuthenticateRepositoryTest {

    private val subject = ClientBAuthenticateRepository()

    @Test
    fun `auth operations complete`() = runTest {
        subject.loginUser(
            email = "lead@streamcore.tv",
            password = "password",
        )
        subject.loginUserWithQR(qrCode = "qr-code")
        subject.forgotPassword(
            email = "lead@streamcore.tv",
            otp = null,
        )
        subject.logoutUser()
    }
}
