package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/**
 * Account authentication. Bootstrap through [StreamCoreClient.bootstrap] and observe [StreamCoreClient.context].
 * Successful login establishes account identity; profile authorization requires a separate entry decision.
 * Coroutine cancellation propagates, including when provider state has already changed; observe context for that state.
 */
interface AuthService {
    /** Validates credentials and trims the identifier; password contents are not normalized. */
    suspend fun login(identifier: String, password: String): StreamCoreResult<Unit>
    /** Returns `StreamCoreError.Unsupported` when QR authentication is not a provider capability. */
    suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit>
    /**
     * Logs out while retaining account-owned library/history/progress. A transient provider failure preserves
     * the active session for retry; authoritative provider logout clears SDK authorization even if cleanup is cancelled.
     */
    suspend fun logout(): StreamCoreResult<Unit>
    /** Provider-specific recovery; unsupported providers return `StreamCoreError.Unsupported`. */
    suspend fun recoverPassword(email: String, otp: String? = null): StreamCoreResult<Unit>
}
