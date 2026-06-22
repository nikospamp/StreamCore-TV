package com.pampoukidis.streamcoretv.data.client.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ClientBAuthenticateRepository @Inject constructor() : AuthenticateRepository {

    private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
    override val authState: StateFlow<AuthStateModel> = _authState

    override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
        _authState.value = AuthStateModel.LoggedOut
        return AppResult.Success(AuthStateModel.LoggedOut)
    }

    override suspend fun loginUser(identifier: String, password: String): AppResult<Unit> {
        _authState.value = AuthStateModel.LoggedIn(account = null)
        return AppResult.Success(Unit)
    }

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> {
        _authState.value = AuthStateModel.LoggedIn(account = null)
        return AppResult.Success(Unit)
    }

    override suspend fun logoutUser(): AppResult<Unit> {
        _authState.value = AuthStateModel.LoggedOut
        return AppResult.Success(Unit)
    }

    override suspend fun forgotPassword(email: String, otp: String?): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}