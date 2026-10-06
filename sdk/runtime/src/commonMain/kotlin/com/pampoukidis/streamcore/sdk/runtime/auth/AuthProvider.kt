package com.pampoukidis.streamcore.sdk.runtime.auth

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import kotlinx.coroutines.flow.StateFlow

/** Backend authentication and credentials. Runtime owns shared validation and observable account/profile context. */
interface AuthProvider {

    val authState: StateFlow<StreamCoreAuthState>

    /** Restores and validates provider-owned credentials; runtime coordinates SDK context and local preparation. */
    suspend fun restoreSession(): StreamCoreResult<StreamCoreAuthState>

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
}
