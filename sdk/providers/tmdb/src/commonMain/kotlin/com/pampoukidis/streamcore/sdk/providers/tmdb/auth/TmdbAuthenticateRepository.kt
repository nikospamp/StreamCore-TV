package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbApi
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcore.sdk.runtime.auth.AuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class TmdbAuthenticateRepository constructor(
    private val tmdbApi: TmdbApi,
    private val callExecutor: TmdbCallExecutor,
    private val authStore: TmdbAuthStore,
    private val accountId: String,
) : AuthProvider {

    override suspend fun invalidateSession() {
        pendingInvalidatedSessionId = authStore.currentSessionId()
        _authState.value = StreamCoreAuthState.LoggedOut
        authStore.clear()
        pendingInvalidatedSessionId = null
    }

    private val _authState = MutableStateFlow<StreamCoreAuthState>(StreamCoreAuthState.LoggedOut)
    override val authState: StateFlow<StreamCoreAuthState> = _authState.asStateFlow()

    // Only a revoked or authoritatively rejected id can bypass remote deletion on retry.
    private var pendingInvalidatedSessionId: String? = null

    override suspend fun restoreSession(): StreamCoreResult<StreamCoreAuthState> {
        val sessionId = when (
            val result = localAuthOperation(BOOTSTRAP_OPERATION, BOOTSTRAP_LOCAL_READ_FAILED_CODE) {
                authStore.currentSessionId()
            }
        ) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> {
                _authState.value = StreamCoreAuthState.LoggedOut
                return result
            }
        }
        if (sessionId == null) {
            pendingInvalidatedSessionId = null
            _authState.value = StreamCoreAuthState.LoggedOut
            return StreamCoreResult.Success(StreamCoreAuthState.LoggedOut)
        }
        if (sessionId == pendingInvalidatedSessionId) {
            _authState.value = StreamCoreAuthState.LoggedOut
            return when (val result = clearSessionForLogout()) {
                is StreamCoreResult.Success -> StreamCoreResult.Success(StreamCoreAuthState.LoggedOut)
                is StreamCoreResult.Failure -> result
            }
        }

        return when (
            val result = callExecutor.execute(operation = BOOTSTRAP_OPERATION) {
                tmdbApi.getMovieAccountStates(
                    movieId = BOOTSTRAP_SESSION_VALIDATION_MOVIE_ID,
                    sessionId = sessionId,
                )
            }
        ) {
            is StreamCoreResult.Success -> {
                when (val accountResult = loadVerifiedAccount(sessionId = sessionId)) {
                    is StreamCoreResult.Success -> {
                        val authState = StreamCoreAuthState.LoggedIn(account = accountResult.value)
                        when (
                            val saveResult = localAuthOperation(
                                BOOTSTRAP_OPERATION,
                                BOOTSTRAP_LOCAL_WRITE_FAILED_CODE,
                            ) {
                                authStore.saveSession(
                                    sessionId = sessionId,
                                    account = accountResult.value,
                                )
                            }
                        ) {
                            is StreamCoreResult.Success -> Unit
                            is StreamCoreResult.Failure -> {
                                _authState.value = StreamCoreAuthState.LoggedOut
                                return saveResult
                            }
                        }
                        pendingInvalidatedSessionId = null
                        _authState.value = authState
                        StreamCoreResult.Success(authState)
                    }

                    is StreamCoreResult.Failure -> bootstrapFailure(error = accountResult.error, sessionId = sessionId)
                }
            }

            is StreamCoreResult.Failure -> bootstrapFailure(error = result.error, sessionId = sessionId)
        }
    }

    override suspend fun login(
        identifier: String,
        password: String,
    ): StreamCoreResult<Unit> {
        val validatedToken = when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                val requestToken = tmdbApi.createRequestToken()
                    .requireRequestToken(backendCode = CREATE_REQUEST_TOKEN_FAILED_CODE)

                tmdbApi.validateRequestTokenWithLogin(
                    identifier = identifier,
                    password = password,
                    requestToken = requestToken,
                ).requireRequestToken(backendCode = VALIDATE_LOGIN_FAILED_CODE)
            }
        ) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }

        when (val result = revokeRetainedSession()) {
            is StreamCoreResult.Success -> Unit
            is StreamCoreResult.Failure -> return result
        }

        val sessionId = when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                tmdbApi.createSession(requestToken = validatedToken).requireSessionId()
            }
        ) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }

        val accountResult = try {
            loadVerifiedAccount(sessionId = sessionId)
        } catch (exception: CancellationException) {
            compensateUncommittedSession(sessionId = sessionId)
            throw exception
        }
        return when (accountResult) {
            is StreamCoreResult.Success -> persistNewSession(
                sessionId = sessionId,
                account = accountResult.value,
            )

            is StreamCoreResult.Failure -> {
                compensateUncommittedSession(sessionId = sessionId)
                accountResult
            }
        }
    }

    private suspend fun revokeRetainedSession(): StreamCoreResult<Unit> {
        val retainedSessionId = when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                authStore.currentSessionId()
            }
        ) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }
        if (retainedSessionId == null) {
            pendingInvalidatedSessionId = null
            return StreamCoreResult.Success(Unit)
        }

        if (retainedSessionId != pendingInvalidatedSessionId) {
            when (
                val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                    val response = tmdbApi.deleteSession(sessionId = retainedSessionId)
                    if (!response.success) {
                        throw TmdbAuthenticationFailureException(
                            backendCode = REPLACE_SESSION_DELETE_FAILED_CODE,
                            message = "TMDB did not revoke the retained session.",
                        )
                    }
                }
            ) {
                is StreamCoreResult.Success -> pendingInvalidatedSessionId = retainedSessionId
                is StreamCoreResult.Failure -> return result
            }
        }

        return when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                authStore.clear()
            }
        ) {
            is StreamCoreResult.Success -> {
                pendingInvalidatedSessionId = null
                _authState.value = StreamCoreAuthState.LoggedOut
                StreamCoreResult.Success(Unit)
            }

            is StreamCoreResult.Failure -> result
        }
    }

    private suspend fun persistNewSession(
        sessionId: String,
        account: StreamCoreAuthAccount,
    ): StreamCoreResult<Unit> {
        return try {
            when (
                val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                    authStore.saveSession(
                        sessionId = sessionId,
                        account = account,
                    )
                }
            ) {
                is StreamCoreResult.Success -> {
                    _authState.value = StreamCoreAuthState.LoggedIn(account = account)
                    StreamCoreResult.Success(Unit)
                }

                is StreamCoreResult.Failure -> {
                    when (
                        reconcileSessionAfterPersistenceFailure(
                            sessionId = sessionId,
                            account = account,
                        )
                    ) {
                        SessionCommitStatus.Committed -> StreamCoreResult.Success(Unit)
                        SessionCommitStatus.NotCommitted,
                        SessionCommitStatus.Indeterminate -> result
                    }
                }
            }
        } catch (exception: CancellationException) {
            reconcileSessionAfterPersistenceFailure(
                sessionId = sessionId,
                account = account,
            )
            throw exception
        }
    }

    override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("loginUserWithQR"))
    }

    override suspend fun logout(): StreamCoreResult<Unit> {
        val sessionId = when (val result = readSessionForLogout()) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }

        if (sessionId != null && sessionId != pendingInvalidatedSessionId) {
            when (val result = callExecutor.execute(operation = LOGOUT_OPERATION) {
                val response = tmdbApi.deleteSession(sessionId = sessionId)
                if (!response.success) {
                    throw TmdbAuthenticationFailureException(
                        backendCode = DELETE_SESSION_FAILED_CODE,
                        message = "TMDB did not delete the session.",
                    )
                }
            }) {
                is StreamCoreResult.Success -> pendingInvalidatedSessionId = sessionId
                is StreamCoreResult.Failure -> {
                    if (result.error is StreamCoreError.Unauthorized || result.error is StreamCoreError.SessionExpired) {
                        return clearSessionAndFail(error = result.error.toBootstrapFailure(), sessionId = sessionId)
                    }
                    return result
                }
            }
        }

        when (val result = clearSessionForLogout()) {
            is StreamCoreResult.Success -> Unit
            is StreamCoreResult.Failure -> return result
        }
        _authState.value = StreamCoreAuthState.LoggedOut
        return StreamCoreResult.Success(Unit)
    }

    private suspend fun readSessionForLogout(): StreamCoreResult<String?> {
        return localAuthOperation(LOGOUT_OPERATION, LOGOUT_LOCAL_READ_FAILED_CODE) {
            authStore.currentSessionId()
        }
    }

    private suspend fun clearSessionForLogout(): StreamCoreResult<Unit> {
        return localAuthOperation(LOGOUT_OPERATION, LOGOUT_LOCAL_CLEAR_FAILED_CODE) {
            authStore.clear()
            pendingInvalidatedSessionId = null
        }
    }

    private suspend fun <T> localAuthOperation(
        operation: String,
        backendCode: String,
        block: suspend () -> T,
    ): StreamCoreResult<T> {
        return try {
            StreamCoreResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            StreamCoreResult.Failure(
                StreamCoreError.Unknown(
                    source = StreamCoreErrorSource(
                        client = CLIENT,
                        operation = operation,
                        backendCode = backendCode,
                    ),
                ),
            )
        }
    }

    override suspend fun recoverPassword(
        email: String,
        otp: String?,
    ): StreamCoreResult<Unit> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("forgotPassword"))
    }

    private suspend fun loadVerifiedAccount(sessionId: String): StreamCoreResult<StreamCoreAuthAccount> {
        return callExecutor.execute(operation = GET_ACCOUNT_DETAILS_OPERATION) {
            // Discover identity from the session. A configured restriction is never identity.
            val account = tmdbApi.getAccountDetails(
                accountId = null,
                sessionId = sessionId,
            ).toModel()
            if (account.id.isBlank() || (accountId.isNotBlank() && account.id != accountId)) {
                throw TmdbAuthenticationFailureException(
                    backendCode = ACCOUNT_ID_MISMATCH_CODE,
                    message = "TMDB returned account details for a different account.",
                )
            }
            account
        }
    }

    private fun invalidAccountIdFailure(): StreamCoreResult.Failure {
        return StreamCoreResult.Failure(
            StreamCoreError.Unknown(
                source = StreamCoreErrorSource(
                    client = CLIENT,
                    operation = GET_ACCOUNT_DETAILS_OPERATION,
                    backendCode = INVALID_ACCOUNT_ID_CONFIGURATION_CODE,
                    backendMessage = "Configured TMDB account id is not numeric.",
                ),
            ),
        )
    }

    private fun TmdbRequestTokenResponseDto.requireRequestToken(
        backendCode: String,
    ): String {
        if (!success || requestToken.isBlank()) {
            throw TmdbAuthenticationFailureException(
                backendCode = backendCode,
                message = "TMDB did not return a valid request token.",
            )
        }

        return requestToken
    }

    private fun TmdbSessionResponseDto.requireSessionId(): String {
        if (!success || sessionId.isBlank()) {
            throw TmdbAuthenticationFailureException(
                backendCode = CREATE_SESSION_FAILED_CODE,
                message = "TMDB did not return a valid session.",
            )
        }

        return sessionId
    }

    private fun TmdbAccountDetailsDto.toModel(): StreamCoreAuthAccount {
        return StreamCoreAuthAccount(
            id = id.toString(),
            username = username,
            displayName = displayName?.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun <T> clearSessionAndFail(error: StreamCoreError, sessionId: String): StreamCoreResult<T> {
        // An authoritative rejection remains primary even when local cleanup is unavailable.
        pendingInvalidatedSessionId = sessionId
        _authState.value = StreamCoreAuthState.LoggedOut
        clearSessionForLogout()
        return StreamCoreResult.Failure(error)
    }

    private suspend fun compensateUncommittedSession(sessionId: String) {
        withContext(NonCancellable) {
            val persistedSessionId = try {
                authStore.currentSessionId()
            } catch (_: Throwable) {
                return@withContext
            }
            if (persistedSessionId == sessionId) {
                return@withContext
            }

            try {
                val response = tmdbApi.deleteSession(sessionId = sessionId)
                if (!response.success) {
                    return@withContext
                }
            } catch (_: Throwable) {
                // Best effort only: the login failure or cancellation remains primary.
            }
        }
    }

    private suspend fun reconcileSessionAfterPersistenceFailure(
        sessionId: String,
        account: StreamCoreAuthAccount,
    ): SessionCommitStatus {
        return withContext(NonCancellable) {
            val persistedSessionId = try {
                authStore.currentSessionId()
            } catch (_: Throwable) {
                return@withContext SessionCommitStatus.Indeterminate
            }
            if (persistedSessionId == sessionId) {
                _authState.value = StreamCoreAuthState.LoggedIn(account = account)
                return@withContext SessionCommitStatus.Committed
            }

            try {
                val response = tmdbApi.deleteSession(sessionId = sessionId)
                if (!response.success) {
                    return@withContext SessionCommitStatus.NotCommitted
                }
            } catch (_: Throwable) {
                // Best effort only: the persistence failure or cancellation remains primary.
            }
            SessionCommitStatus.NotCommitted
        }
    }

    private suspend fun <T> bootstrapFailure(error: StreamCoreError, sessionId: String): StreamCoreResult<T> {
        _authState.value = StreamCoreAuthState.LoggedOut
        val normalizedError = error.toBootstrapFailure()
        if (normalizedError.invalidatesPersistedSession()) {
            return clearSessionAndFail(error = normalizedError, sessionId = sessionId)
        }
        return StreamCoreResult.Failure(normalizedError)
    }

    private fun StreamCoreError.invalidatesPersistedSession(): Boolean {
        return this is StreamCoreError.Authentication ||
            this is StreamCoreError.Unauthorized ||
            this is StreamCoreError.SessionExpired
    }

    private fun StreamCoreError.toBootstrapFailure(): StreamCoreError {
        return when (this) {
            is StreamCoreError.Authentication -> sessionExpiredError(
                backendCode = source?.backendCode,
                backendMessage = source?.backendMessage,
                httpCode = source?.httpCode,
            )

            is StreamCoreError.Unauthorized -> sessionExpiredError(
                backendCode = source?.backendCode,
                backendMessage = source?.backendMessage,
                httpCode = source?.httpCode,
            )

            else -> this
        }
    }

    private fun sessionExpiredError(
        backendCode: String? = null,
        backendMessage: String? = null,
        httpCode: Int? = null,
    ): StreamCoreError.SessionExpired {
        return StreamCoreError.SessionExpired(
            source = StreamCoreErrorSource(
                client = CLIENT,
                operation = BOOTSTRAP_OPERATION,
                httpCode = httpCode,
                backendCode = backendCode,
                backendMessage = backendMessage,
            ),
        )
    }

    private companion object {
        const val CLIENT = "tmdb"
        const val BOOTSTRAP_OPERATION = "bootstrapAuth"
        const val LOGIN_OPERATION = "loginUser"
        const val LOGOUT_OPERATION = "logoutUser"
        const val GET_ACCOUNT_DETAILS_OPERATION = "getAccountDetails"

        // Known stable TMDB movie id used only to validate the user session.
        const val BOOTSTRAP_SESSION_VALIDATION_MOVIE_ID = 550
        const val CREATE_REQUEST_TOKEN_FAILED_CODE = "CREATE_REQUEST_TOKEN_FAILED"
        const val VALIDATE_LOGIN_FAILED_CODE = "VALIDATE_LOGIN_FAILED"
        const val CREATE_SESSION_FAILED_CODE = "CREATE_SESSION_FAILED"
        const val DELETE_SESSION_FAILED_CODE = "DELETE_SESSION_FAILED"
        const val REPLACE_SESSION_DELETE_FAILED_CODE = "REPLACE_SESSION_DELETE_FAILED"
        const val ACCOUNT_ID_MISMATCH_CODE = "ACCOUNT_ID_MISMATCH"
        const val INVALID_ACCOUNT_ID_CONFIGURATION_CODE = "INVALID_ACCOUNT_ID_CONFIGURATION"
        const val LOGOUT_LOCAL_READ_FAILED_CODE = "LOGOUT_LOCAL_READ_FAILED"
        const val LOGOUT_LOCAL_CLEAR_FAILED_CODE = "LOGOUT_LOCAL_CLEAR_FAILED"
        const val BOOTSTRAP_LOCAL_READ_FAILED_CODE = "BOOTSTRAP_LOCAL_READ_FAILED"
        const val BOOTSTRAP_LOCAL_WRITE_FAILED_CODE = "BOOTSTRAP_LOCAL_WRITE_FAILED"
    }
}

private enum class SessionCommitStatus {
    Committed,
    NotCommitted,
    Indeterminate,
}
