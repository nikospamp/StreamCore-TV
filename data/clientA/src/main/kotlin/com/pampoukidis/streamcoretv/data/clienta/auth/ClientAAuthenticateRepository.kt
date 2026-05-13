package com.pampoukidis.streamcoretv.data.clienta.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class ClientAAuthenticateRepository @Inject constructor() : AuthenticateRepository {

    override suspend fun loginUser(
        email: String,
        password: String,
    ) {
        delay(LOGIN_DELAY_MILLIS)

        if (email != MOCK_EMAIL || password != MOCK_PASSWORD) {
            throw IllegalArgumentException("Invalid email or password")
        }
    }

    override suspend fun loginUserWithQR(qrCode: String) = Unit

    override suspend fun logoutUser() = Unit

    override suspend fun forgotPassword(
        email: String,
        otp: String?,
    ) = Unit

    private companion object {
        const val MOCK_EMAIL = "nikos@mail.com"
        const val MOCK_PASSWORD = "123456"
        const val LOGIN_DELAY_MILLIS = 4_000L
    }
}
