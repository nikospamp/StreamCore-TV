package com.pampoukidis.streamcoretv.client.clientb.data.auth

import com.pampoukidis.streamcore.sdk.runtime.integration.provider.AuthProvider
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ClientBAuthenticateRepository constructor(
    private val authStore: ClientBAuthStore,
) : AuthProvider {

    override suspend fun invalidateSession() {
        authStore.clear()
        _authState.value = StreamCoreAuthState.LoggedOut
    }

    override suspend fun legacyAccountId(): String? {
        return authStore.currentAccountId()
    }

    private val _authState = MutableStateFlow<StreamCoreAuthState>(StreamCoreAuthState.LoggedOut)
    override val authState: StateFlow<StreamCoreAuthState> = _authState

    override suspend fun bootstrapAuth(): StreamCoreResult<StreamCoreAuthState> {
        return when (val result = executeStore(BOOTSTRAP_OPERATION) { authStore.currentAccountId() }) {
            is StreamCoreResult.Success -> {
                val accountId = result.value
                val state = if (accountId != null) {
                    StreamCoreAuthState.LoggedIn(account = account(accountId))
                } else {
                    StreamCoreAuthState.LoggedOut
                }
                _authState.value = state
                StreamCoreResult.Success(state)
            }

            is StreamCoreResult.Failure -> result
        }
    }

    override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> {
        return persistLoggedInState(operation = LOGIN_OPERATION, id = "clientb:" + identifier.trim().lowercase())
    }

    override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("loginUserWithQR"))
    }

    override suspend fun logout(): StreamCoreResult<Unit> {
        return when (val result = executeStore(LOGOUT_OPERATION) { authStore.clear() }) {
            is StreamCoreResult.Success -> {
                _authState.value = StreamCoreAuthState.LoggedOut
                StreamCoreResult.Success(Unit)
            }

            is StreamCoreResult.Failure -> result
        }
    }

    override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("forgotPassword"))
    }

    private suspend fun persistLoggedInState(operation: String, id: String): StreamCoreResult<Unit> {
        return when (val result = executeStore(operation) { authStore.setAccountId(id) }) {
            is StreamCoreResult.Success -> {
                _authState.value = StreamCoreAuthState.LoggedIn(account = account(id))
                StreamCoreResult.Success(Unit)
            }

            is StreamCoreResult.Failure -> result
        }
    }

    private fun account(id: String): StreamCoreAuthAccount {
        return StreamCoreAuthAccount(id = id, username = id.removePrefix("clientb:"), displayName = null)
    }

    private suspend fun <T> executeStore(
        operation: String,
        block: suspend () -> T,
    ): StreamCoreResult<T> {
        return try {
            StreamCoreResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            StreamCoreResult.Failure(
                StreamCoreError.Unknown(
                    source = StreamCoreErrorSource(
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
