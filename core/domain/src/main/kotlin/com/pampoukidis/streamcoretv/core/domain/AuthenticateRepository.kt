package com.pampoukidis.streamcoretv.core.domain

import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.flow.Flow

interface AuthenticateRepository {

    val authState: Flow<AuthStateModel>

    suspend fun loginUser(
        identifier: String,
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