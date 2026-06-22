package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppAuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts as loading until bootstrap runs`() {
        val repository = FakeAuthenticateRepository()

        val subject = AppAuthViewModel(authenticateRepository = repository)

        assertEquals(AppAuthUiState.Loading, subject.uiState.value)
        assertEquals(0, repository.bootstrapCalls)
    }

    @Test
    fun `emits ready logged in after valid bootstrap`() {
        val authState = AuthStateModel.LoggedIn(
            account = AuthAccountModel(
                id = 548,
                username = "lead",
                displayName = "Lead",
            ),
        )
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Success(authState),
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppAuthUiState.Ready(authState = authState), subject.uiState.value)
        assertEquals(1, repository.bootstrapCalls)
    }

    @Test
    fun `emits ready logged out and error effect after failed bootstrap`() = runTest {
        val error = AppError.Network()
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Failure(error),
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(authState = AuthStateModel.LoggedOut),
            subject.uiState.value,
        )
        assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())
        assertEquals(1, repository.bootstrapCalls)
    }

    private class FakeAuthenticateRepository(
        private val bootstrapResult: AppResult<AuthStateModel> =
            AppResult.Success(AuthStateModel.LoggedOut),
    ) : AuthenticateRepository {

        private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
        override val authState: StateFlow<AuthStateModel> = _authState

        var bootstrapCalls = 0
            private set

        override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
            bootstrapCalls += 1

            when (bootstrapResult) {
                is AppResult.Success -> {
                    _authState.value = bootstrapResult.value
                }

                is AppResult.Failure -> {
                    _authState.value = AuthStateModel.LoggedOut
                }
            }

            return bootstrapResult
        }

        override suspend fun loginUser(
            identifier: String,
            password: String,
        ): AppResult<Unit> {
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

        override suspend fun forgotPassword(
            email: String,
            otp: String?,
        ): AppResult<Unit> {
            return AppResult.Success(Unit)
        }
    }
}
