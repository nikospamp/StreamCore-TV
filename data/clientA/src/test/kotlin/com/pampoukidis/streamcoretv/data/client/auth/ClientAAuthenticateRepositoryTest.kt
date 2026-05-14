package com.pampoukidis.streamcoretv.data.client.auth

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientAAuthenticateRepositoryTest {

    private val subject = ClientAAuthenticateRepository()

    @Test
    fun `login succeeds after delay with mock credentials`() = runTest {
        val result = subject.loginUser(
            email = "nikos@mail.com",
            password = "123456",
        )

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(4_000L, currentTime)
    }

    @Test
    fun `login fails after delay with invalid credentials`() = runTest {
        val result = subject.loginUser(
            email = "lead@streamcore.tv",
            password = "password",
        )

        assertEquals(4_000L, currentTime)
        assertEquals(
            AppResult.Failure(
                AppError.Authentication(
                    source = ErrorSource(
                        client = "clientA",
                        operation = "loginUser",
                        backendCode = "INVALID_CREDENTIALS",
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `secondary auth operations complete`() = runTest {
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
