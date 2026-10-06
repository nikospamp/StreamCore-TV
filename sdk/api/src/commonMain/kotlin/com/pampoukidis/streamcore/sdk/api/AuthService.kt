package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/**
 * Account authentication. Restore saved authentication through [restoreSession] and observe [StreamCoreClient.context].
 * Successful login establishes account identity; profile authorization requires a separate entry decision.
 * Coroutine cancellation propagates, including when provider state has already changed; observe context for that state.
 */
interface AuthService {
    /**
     * Restores and validates saved account authentication, never a persisted profile authorization.
     * Migration and local preparation happen internally. Factory construction performs no network requests.
     * Once authentication is initialized, returns the current context without repeating restoration.
     * Handle failures before starting dependent work; retry explicitly when appropriate.
     * Enter a profile through [ProfileService.beginEntry] or selection before accessing its content.
     * Explicit [login] can initialize authentication directly; it does not require restoration first.
     */
    suspend fun restoreSession(): StreamCoreResult<StreamCoreContext>

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
