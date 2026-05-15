package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.login.domain.LoginFieldError
import com.pampoukidis.streamcoretv.feature.login.domain.LoginWithCredentialsUseCase
import com.pampoukidis.streamcoretv.feature.login.domain.ValidateLoginCredentialsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `valid credentials enable submit`() {
        val subject = loginViewModel()

        subject.onAction(LoginAction.EmailChanged("lead@streamcore.tv"))
        subject.onAction(LoginAction.PasswordChanged("password"))

        assertTrue(subject.uiState.value.isSubmitEnabled)
        assertNull(subject.uiState.value.emailError)
        assertNull(subject.uiState.value.passwordError)
    }

    @Test
    fun `invalid submit surfaces validation errors`() {
        val subject = loginViewModel()

        subject.onAction(LoginAction.Submit)

        val state = subject.uiState.value
        assertEquals(LoginFieldError.Required, state.emailError)
        assertEquals(LoginFieldError.Required, state.passwordError)
        assertFalse(state.isSubmitEnabled)
        assertFalse(state.isLoading)
    }

    @Test
    fun `submit logs in with trimmed credentials and emits success`() = runTest {
        val repository = FakeAuthenticateRepository()
        val subject = loginViewModel(repository)
        val effect = async { subject.effects.first() }
        runCurrent()

        subject.onAction(LoginAction.EmailChanged(" lead@streamcore.tv "))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals("lead@streamcore.tv", repository.loginEmail)
        assertEquals("password", repository.loginPassword)
        assertEquals(LoginEffect.LoginSucceeded, effect.await())
        assertFalse(subject.uiState.value.isLoading)
        assertTrue(subject.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `submit failure emits error effect and re-enables submit`() = runTest {
        val error = AppError.Authentication()
        val subject = loginViewModel(
            authenticateRepository = FakeAuthenticateRepository(
                loginResult = AppResult.Failure(error),
            ),
        )
        val effect = async { subject.effects.first() }
        runCurrent()

        subject.onAction(LoginAction.EmailChanged("lead@streamcore.tv"))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals(LoginEffect.ShowError(error), effect.await())
        assertFalse(subject.uiState.value.isLoading)
        assertTrue(subject.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `effect emitted before collection is delivered to next collector`() = runTest {
        val subject = loginViewModel()

        subject.onAction(LoginAction.Help)
        runCurrent()

        assertEquals(LoginEffect.Help, subject.effects.first())
    }

    private fun loginViewModel(
        authenticateRepository: AuthenticateRepository = FakeAuthenticateRepository(),
    ) = LoginViewModel(
        validateCredentials = ValidateLoginCredentialsUseCase(),
        loginWithCredentials = LoginWithCredentialsUseCase(authenticateRepository),
    )

    private class FakeAuthenticateRepository(
        private val loginResult: AppResult<Unit> = AppResult.Success(Unit),
    ) : AuthenticateRepository {

        var loginEmail: String? = null
            private set

        var loginPassword: String? = null
            private set

        override suspend fun loginUser(
            email: String,
            password: String,
        ): AppResult<Unit> {
            loginEmail = email
            loginPassword = password
            return loginResult
        }

        override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun logoutUser(): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun forgotPassword(
            email: String,
            otp: String?,
        ): AppResult<Unit> = AppResult.Success(Unit)
    }
}
