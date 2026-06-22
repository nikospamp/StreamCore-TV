package com.pampoukidis.streamcoretv.data.tmdb.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbAccountDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbSessionResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbApi
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbAuthenticationFailureException
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbCallExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named

class TmdbAuthenticateRepository @Inject internal constructor(
    private val tmdbApi: TmdbApi,
    private val callExecutor: TmdbCallExecutor,
    private val authStore: TmdbAuthStore,
    @param:Named(TMDB_ACCOUNT_ID)
    private val accountId: String,
) : AuthenticateRepository {

    private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
    override val authState: StateFlow<AuthStateModel> = _authState.asStateFlow()

    override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
        val sessionId = authStore.currentSessionId()
        if (sessionId == null) {
            _authState.value = AuthStateModel.LoggedOut
            return AppResult.Success(AuthStateModel.LoggedOut)
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
                val account = loadAccount(sessionId = sessionId)
                val authState = AuthStateModel.LoggedIn(account = account)
                authStore.saveSession(
                    sessionId = sessionId,
                    account = account,
                )
                _authState.value = authState
                AppResult.Success(authState)
            }

            is AppResult.Failure -> clearSessionAndFail(
                error = result.error.toBootstrapFailure(),
            )
        }
    }

    override suspend fun loginUser(
        identifier: String,
        password: String,
    ): AppResult<Unit> {
        return callExecutor.execute(operation = LOGIN_OPERATION) {
            val requestToken = tmdbApi.createRequestToken()
                .requireRequestToken(backendCode = CREATE_REQUEST_TOKEN_FAILED_CODE)

            val validatedToken = tmdbApi.validateRequestTokenWithLogin(
                identifier = identifier,
                password = password,
                requestToken = requestToken,
            ).requireRequestToken(backendCode = VALIDATE_LOGIN_FAILED_CODE)

            val sessionId = tmdbApi.createSession(requestToken = validatedToken)
                .requireSessionId()

            val account = loadAccount(sessionId = sessionId)

            authStore.saveSession(
                sessionId = sessionId,
                account = account,
            )
            _authState.value = AuthStateModel.LoggedIn(account = account)
        }
    }

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun logoutUser(): AppResult<Unit> {
        authStore.currentSessionId()?.let { sessionId ->
            callExecutor.execute(operation = LOGOUT_OPERATION) {
                val response = tmdbApi.deleteSession(sessionId = sessionId)
                if (!response.success) {
                    throw TmdbAuthenticationFailureException(
                        backendCode = DELETE_SESSION_FAILED_CODE,
                        message = "TMDB did not delete the session.",
                    )
                }
            }
        }

        authStore.clear()
        _authState.value = AuthStateModel.LoggedOut
        return AppResult.Success(Unit)
    }

    override suspend fun forgotPassword(
        email: String,
        otp: String?,
    ): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    private suspend fun loadAccount(sessionId: String): AuthAccountModel? {
        val accountId = accountId.toIntOrNull() ?: return null

        return when (
            val result = callExecutor.execute(operation = GET_ACCOUNT_DETAILS_OPERATION) {
                tmdbApi.getAccountDetails(
                    accountId = accountId,
                    sessionId = sessionId,
                ).toModel()
            }
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> null
        }
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

    private suspend fun <T> clearSessionAndFail(error: AppError): AppResult<T> {
        authStore.clear()
        _authState.value = AuthStateModel.LoggedOut
        return AppResult.Failure(error)
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
        const val TMDB_ACCOUNT_ID = "tmdbAccountId"
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
    }
}
