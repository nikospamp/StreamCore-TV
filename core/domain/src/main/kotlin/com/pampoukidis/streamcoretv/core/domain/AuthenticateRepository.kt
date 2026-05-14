package com.pampoukidis.streamcoretv.core.domain

import com.pampoukidis.streamcoretv.core.model.error.AppResult

interface AuthenticateRepository {

    suspend fun loginUser(
        email: String,
        password: String
    ): AppResult<Unit>

    suspend fun loginUserWithQR(
        qrCode: String
    ): AppResult<Unit>

    suspend fun logoutUser(): AppResult<Unit>

    suspend fun forgotPassword(
        email: String,
        otp: String? = null
    ): AppResult<Unit>
}