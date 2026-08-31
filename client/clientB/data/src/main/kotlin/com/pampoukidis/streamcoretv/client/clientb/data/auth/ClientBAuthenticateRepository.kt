package com.pampoukidis.streamcoretv.client.clientb.data.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ClientBAuthenticateRepository @Inject internal constructor(
    private val authStore: ClientBAuthStore,
) : AuthenticateRepository {

    private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
    override val authState: StateFlow<AuthStateModel> = _authState

    override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
        return when (val result = executeStore(BOOTSTRAP_OPERATION) { authStore.isLoggedIn() }) {
            is AppResult.Success -> {
                val state = if (result.value) {
                    AuthStateModel.LoggedIn(account = null)
                } else {
                    AuthStateModel.LoggedOut
                }
                _authState.value = state
                AppResult.Success(state)
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun loginUser(identifier: String, password: String): AppResult<Unit> {
        return persistLoggedInState(operation = LOGIN_OPERATION)
    }

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> {
        return persistLoggedInState(operation = QR_LOGIN_OPERATION)
    }

    override suspend fun logoutUser(): AppResult<Unit> {
        return when (val result = executeStore(LOGOUT_OPERATION) { authStore.clear() }) {
            is AppResult.Success -> {
                _authState.value = AuthStateModel.LoggedOut
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun forgotPassword(email: String, otp: String?): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    private suspend fun persistLoggedInState(operation: String): AppResult<Unit> {
        return when (val result = executeStore(operation) { authStore.setLoggedIn() }) {
            is AppResult.Success -> {
                _authState.value = AuthStateModel.LoggedIn(account = null)
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }
    }

    private suspend fun <T> executeStore(
        operation: String,
        block: suspend () -> T,
    ): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            AppResult.Failure(
                AppError.Unknown(
                    source = ErrorSource(
                        client = CLIENT,
                        operation = operation,
                        backendMessage = throwable.message,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val CLIENT = "clientB"
        const val BOOTSTRAP_OPERATION = "bootstrapAuth"
        const val LOGIN_OPERATION = "loginUser"
        const val QR_LOGIN_OPERATION = "loginUserWithQR"
        const val LOGOUT_OPERATION = "logoutUser"
    }
}
