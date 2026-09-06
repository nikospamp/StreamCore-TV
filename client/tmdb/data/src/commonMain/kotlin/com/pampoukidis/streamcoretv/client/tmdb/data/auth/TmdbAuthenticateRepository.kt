package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbAccountDetailsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbSessionResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbAuthenticationFailureException
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TmdbAuthenticateRepository internal constructor(
    private val tmdbApi: TmdbApi,
    private val callExecutor: TmdbCallExecutor,
    private val authStore: TmdbAuthStore,
    private val accountId: String,
) : AuthenticateRepository {

    private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
    override val authState: StateFlow<AuthStateModel> = _authState.asStateFlow()

    // Only a revoked or authoritatively rejected id can bypass remote deletion on retry.
    private var pendingInvalidatedSessionId: String? = null

    override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
        val sessionId = when (
            val result = localAuthOperation(BOOTSTRAP_OPERATION, BOOTSTRAP_LOCAL_READ_FAILED_CODE) {
                authStore.currentSessionId()
            }
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> {
                _authState.value = AuthStateModel.LoggedOut
                return result
            }
        }
        if (sessionId == null) {
            pendingInvalidatedSessionId = null
            _authState.value = AuthStateModel.LoggedOut
            return AppResult.Success(AuthStateModel.LoggedOut)
        }
        if (sessionId == pendingInvalidatedSessionId) {
            _authState.value = AuthStateModel.LoggedOut
            return when (val result = clearSessionForLogout()) {
                is AppResult.Success -> AppResult.Success(AuthStateModel.LoggedOut)
                is AppResult.Failure -> result
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
            is AppResult.Success -> {
                when (val accountResult = loadVerifiedAccount(sessionId = sessionId)) {
                    is AppResult.Success -> {
                        val authState = AuthStateModel.LoggedIn(account = accountResult.value)
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
                            is AppResult.Success -> Unit
                            is AppResult.Failure -> {
                                _authState.value = AuthStateModel.LoggedOut
                                return saveResult
                            }
                        }
                        pendingInvalidatedSessionId = null
                        _authState.value = authState
                        AppResult.Success(authState)
                    }

                    is AppResult.Failure -> bootstrapFailure(error = accountResult.error, sessionId = sessionId)
                }
            }

            is AppResult.Failure -> bootstrapFailure(error = result.error, sessionId = sessionId)
        }
    }

    override suspend fun loginUser(
        identifier: String,
        password: String,
    ): AppResult<Unit> {
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
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        when (val result = revokeRetainedSession()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return result
        }

        val sessionId = when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                tmdbApi.createSession(requestToken = validatedToken).requireSessionId()
            }
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        val accountResult = try {
            loadVerifiedAccount(sessionId = sessionId)
        } catch (exception: CancellationException) {
            compensateUncommittedSession(sessionId = sessionId)
            throw exception
        }
        return when (accountResult) {
            is AppResult.Success -> persistNewSession(
                sessionId = sessionId,
                account = accountResult.value,
            )

            is AppResult.Failure -> {
                compensateUncommittedSession(sessionId = sessionId)
                accountResult
            }
        }
    }

    private suspend fun revokeRetainedSession(): AppResult<Unit> {
        val retainedSessionId = when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                authStore.currentSessionId()
            }
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        if (retainedSessionId == null) {
            pendingInvalidatedSessionId = null
            return AppResult.Success(Unit)
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
                is AppResult.Success -> pendingInvalidatedSessionId = retainedSessionId
                is AppResult.Failure -> return result
            }
        }

        return when (
            val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                authStore.clear()
            }
        ) {
            is AppResult.Success -> {
                pendingInvalidatedSessionId = null
                _authState.value = AuthStateModel.LoggedOut
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }
    }

    private suspend fun persistNewSession(
        sessionId: String,
        account: AuthAccountModel?,
    ): AppResult<Unit> {
        return try {
            when (
                val result = callExecutor.execute(operation = LOGIN_OPERATION) {
                    authStore.saveSession(
                        sessionId = sessionId,
                        account = account,
                    )
                }
            ) {
                is AppResult.Success -> {
                    _authState.value = AuthStateModel.LoggedIn(account = account)
                    AppResult.Success(Unit)
                }

                is AppResult.Failure -> {
                    when (
                        reconcileSessionAfterPersistenceFailure(
                            sessionId = sessionId,
                            account = account,
                        )
                    ) {
                        SessionCommitStatus.Committed -> AppResult.Success(Unit)
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

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun logoutUser(): AppResult<Unit> {
        val sessionId = when (val result = readSessionForLogout()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
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
                is AppResult.Success -> pendingInvalidatedSessionId = sessionId
                is AppResult.Failure -> {
                    if (result.error is AppError.Unauthorized || result.error is AppError.SessionExpired) {
                        return clearSessionAndFail(error = result.error, sessionId = sessionId)
                    }
                    return result
                }
            }
        }

        when (val result = clearSessionForLogout()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return result
        }
        _authState.value = AuthStateModel.LoggedOut
        return AppResult.Success(Unit)
    }

    private suspend fun readSessionForLogout(): AppResult<String?> {
        return localAuthOperation(LOGOUT_OPERATION, LOGOUT_LOCAL_READ_FAILED_CODE) {
            authStore.currentSessionId()
        }
    }

    private suspend fun clearSessionForLogout(): AppResult<Unit> {
        return localAuthOperation(LOGOUT_OPERATION, LOGOUT_LOCAL_CLEAR_FAILED_CODE) {
            authStore.clear()
            pendingInvalidatedSessionId = null
        }
    }

    private suspend fun <T> localAuthOperation(
        operation: String,
        backendCode: String,
        block: suspend () -> T,
    ): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            AppResult.Failure(
                AppError.Unknown(
                    source = ErrorSource(
                        client = CLIENT,
                        operation = operation,
                        backendCode = backendCode,
                    ),
                ),
            )
        }
    }

    override suspend fun forgotPassword(
        email: String,
        otp: String?,
    ): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    private suspend fun loadVerifiedAccount(sessionId: String): AppResult<AuthAccountModel?> {
        if (accountId.isBlank()) {
            return AppResult.Success(null)
        }

        val expectedAccountId = accountId.toIntOrNull() ?: return invalidAccountIdFailure()
        return callExecutor.execute(operation = GET_ACCOUNT_DETAILS_OPERATION) {
            val account = tmdbApi.getAccountDetails(
                accountId = expectedAccountId,
                sessionId = sessionId,
            ).toModel()
            if (account.id != expectedAccountId) {
                throw TmdbAuthenticationFailureException(
                    backendCode = ACCOUNT_ID_MISMATCH_CODE,
                    message = "TMDB returned account details for a different account.",
                )
            }
            account
        }
    }

    private fun invalidAccountIdFailure(): AppResult.Failure {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
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

    private fun TmdbAccountDetailsDto.toModel(): AuthAccountModel {
        return AuthAccountModel(
            id = id,
            username = username,
            displayName = displayName?.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun <T> clearSessionAndFail(error: AppError, sessionId: String): AppResult<T> {
        // An authoritative rejection remains primary even when local cleanup is unavailable.
        pendingInvalidatedSessionId = sessionId
        _authState.value = AuthStateModel.LoggedOut
        clearSessionForLogout()
        return AppResult.Failure(error)
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
        account: AuthAccountModel?,
    ): SessionCommitStatus {
        return withContext(NonCancellable) {
            val persistedSessionId = try {
                authStore.currentSessionId()
            } catch (_: Throwable) {
                return@withContext SessionCommitStatus.Indeterminate
            }
            if (persistedSessionId == sessionId) {
                _authState.value = AuthStateModel.LoggedIn(account = account)
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

    private suspend fun <T> bootstrapFailure(error: AppError, sessionId: String): AppResult<T> {
        _authState.value = AuthStateModel.LoggedOut
        val normalizedError = error.toBootstrapFailure()
        if (normalizedError.invalidatesPersistedSession()) {
            return clearSessionAndFail(error = normalizedError, sessionId = sessionId)
        }
        return AppResult.Failure(normalizedError)
    }

    private fun AppError.invalidatesPersistedSession(): Boolean {
        return this is AppError.Authentication ||
            this is AppError.Unauthorized ||
            this is AppError.SessionExpired
    }

    private fun AppError.toBootstrapFailure(): AppError {
        return when (this) {
            is AppError.Authentication -> sessionExpiredError(
                backendCode = source?.backendCode,
                backendMessage = source?.backendMessage,
                httpCode = source?.httpCode,
            )

            is AppError.Unauthorized -> sessionExpiredError(
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
    ): AppError.SessionExpired {
        return AppError.SessionExpired(
            source = ErrorSource(
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
