package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                activeProfileId = null,
            ),
            subject.uiState.value,
        )
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
            AppAuthUiState.Ready(
                authState = AuthStateModel.LoggedOut,
                activeProfileId = null,
            ),
            subject.uiState.value,
        )
        assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())
        assertEquals(1, repository.bootstrapCalls)
    }

    @Test
    fun `retains active profile in app state`() {
        val authState = AuthStateModel.LoggedIn(account = null)
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Success(authState),
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        subject.onActiveProfileChanged("profile-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                activeProfileId = "profile-1",
            ),
            subject.uiState.value,
        )
    }

    @Test
    fun `request and dismiss logout update confirmation state`() {
        val authState = AuthStateModel.LoggedIn(account = null)
        val subject = AppAuthViewModel(
            authenticateRepository = FakeAuthenticateRepository(
                bootstrapResult = AppResult.Success(authState),
            ),
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        subject.onAction(AppAuthAction.RequestLogout)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                isLogoutConfirmationVisible = true,
            ),
            subject.uiState.value,
        )

        subject.onAction(AppAuthAction.DismissLogoutConfirmation)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(authState = authState),
            subject.uiState.value,
        )
    }

    @Test
    fun `duplicate logout confirmation submits once while in progress`() {
        val logoutGate = CompletableDeferred<Unit>()
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
            logoutGate = logoutGate,
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.logoutCalls)
        assertEquals(true, (subject.uiState.value as AppAuthUiState.Ready).isLogoutInProgress)

        logoutGate.complete(Unit)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `successful logout clears profile and confirmation state`() {
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        subject.onActiveProfileChanged("profile-1")
        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(authState = AuthStateModel.LoggedOut),
            subject.uiState.value,
        )
        assertEquals(1, repository.logoutCalls)
    }

    @Test
    fun `failed logout preserves profile and session and emits error`() = runTest {
        val error = AppError.Network()
        val authState = AuthStateModel.LoggedIn(account = null)
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Success(authState),
            logoutResult = AppResult.Failure(error),
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        subject.onActiveProfileChanged("profile-1")
        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                activeProfileId = "profile-1",
                isLogoutConfirmationVisible = true,
            ),
            subject.uiState.value,
        )
        assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())
    }

    @Test
    fun `unexpected logout exception clears progress and emits unknown error`() = runTest {
        val authState = AuthStateModel.LoggedIn(account = null)
        val repository = FakeAuthenticateRepository(
            bootstrapResult = AppResult.Success(authState),
            logoutThrowable = IllegalStateException("store failed"),
        )
        val subject = AppAuthViewModel(authenticateRepository = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                isLogoutConfirmationVisible = true,
            ),
            subject.uiState.value,
        )
        val effect = subject.effects.first()
        assertTrue(effect is AppAuthEffect.ShowError)
        assertTrue((effect as AppAuthEffect.ShowError).error is AppError.Unknown)
    }

    private class FakeAuthenticateRepository(
        private val bootstrapResult: AppResult<AuthStateModel> =
            AppResult.Success(AuthStateModel.LoggedOut),
        private val logoutResult: AppResult<Unit> = AppResult.Success(Unit),
        private val logoutGate: CompletableDeferred<Unit>? = null,
        private val logoutThrowable: Throwable? = null,
    ) : AuthenticateRepository {

        private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
        override val authState: StateFlow<AuthStateModel> = _authState

        var bootstrapCalls = 0
            private set

        var logoutCalls = 0
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
            logoutCalls += 1
            logoutGate?.await()
            logoutThrowable?.let { throwable -> throw throwable }
            if (logoutResult is AppResult.Success) {
                _authState.value = AuthStateModel.LoggedOut
            }
            return logoutResult
        }

        override suspend fun forgotPassword(
            email: String,
            otp: String?,
        ): AppResult<Unit> {
            return AppResult.Success(Unit)
        }
    }
}
