package com.pampoukidis.streamcore.sdk.runtime.integration.provider

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import kotlinx.coroutines.flow.StateFlow

/** Backend authentication and credentials. Runtime owns shared validation and observable account/profile context. */
interface AuthProvider {

    val authState: StateFlow<StreamCoreAuthState>

    suspend fun bootstrapAuth(): StreamCoreResult<StreamCoreAuthState>

    suspend fun login(
        identifier: String,
        password: String
    ): StreamCoreResult<Unit>

    suspend fun loginWithQr(
        qrCode: String
    ): StreamCoreResult<Unit>

    suspend fun logout(): StreamCoreResult<Unit>

    suspend fun recoverPassword(
        email: String,
        otp: String? = null
    ): StreamCoreResult<Unit>
    suspend fun invalidateSession()
    suspend fun legacyAccountId(): String? { return null }
}
