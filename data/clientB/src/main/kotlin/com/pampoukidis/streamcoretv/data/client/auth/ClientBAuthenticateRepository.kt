package com.pampoukidis.streamcoretv.data.client.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import javax.inject.Inject

class ClientBAuthenticateRepository @Inject constructor() : AuthenticateRepository {

    override suspend fun loginUser(
        email: String,
        password: String,
    ): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun logoutUser(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun forgotPassword(
        email: String,
        otp: String?,
    ): AppResult<Unit> = AppResult.Success(Unit)
}