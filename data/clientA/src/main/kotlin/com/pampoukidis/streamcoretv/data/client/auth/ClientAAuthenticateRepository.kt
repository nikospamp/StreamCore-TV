package com.pampoukidis.streamcoretv.data.client.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.delay
import javax.inject.Inject

class ClientAAuthenticateRepository @Inject constructor() : AuthenticateRepository {

    override suspend fun loginUser(
        email: String,
        password: String,
    ): AppResult<Unit> {
        delay(LOGIN_DELAY_MILLIS)

        if (email != MOCK_EMAIL || password != MOCK_PASSWORD) {
            return AppResult.Failure(
                AppError.Authentication(
                    source = ErrorSource(
                        client = CLIENT,
                        operation = LOGIN_OPERATION,
                        backendCode = INVALID_CREDENTIALS_CODE,
                    ),
                ),
            )
        }

        return AppResult.Success(Unit)
    }

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun logoutUser(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun forgotPassword(
        email: String,
        otp: String?,
    ): AppResult<Unit> = AppResult.Success(Unit)

    private companion object {
        const val CLIENT = "clientA"
        const val LOGIN_OPERATION = "loginUser"
        const val INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS"
        const val MOCK_EMAIL = "nikos@mail.com"
        const val MOCK_PASSWORD = "123456"
        const val LOGIN_DELAY_MILLIS = 4_000L
    }
}