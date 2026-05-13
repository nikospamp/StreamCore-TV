package com.pampoukidis.streamcoretv.feature.login.common

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginEvent
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginViewModel
import com.pampoukidis.streamcoretv.feature.login.domain.LoginFieldError
import com.pampoukidis.streamcoretv.feature.login.domain.LoginWithCredentialsUseCase
import com.pampoukidis.streamcoretv.feature.login.domain.ValidateLoginCredentialsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
        val event = async { subject.events.first() }
        runCurrent()

        subject.onAction(LoginAction.EmailChanged(" lead@streamcore.tv "))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals("lead@streamcore.tv", repository.loginEmail)
        assertEquals("password", repository.loginPassword)
        assertEquals(LoginEvent.LoginSucceeded, event.await())
        assertFalse(subject.uiState.value.isLoading)
        assertTrue(subject.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `submit failure shows generic error and re-enables submit`() = runTest {
        val subject = loginViewModel(
            authenticateRepository = FakeAuthenticateRepository(
                loginFailure = IllegalStateException("Backend rejected login"),
            ),
        )

        subject.onAction(LoginAction.EmailChanged("lead@streamcore.tv"))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals("", subject.uiState.value.errorMessage)
        assertFalse(subject.uiState.value.isLoading)
        assertTrue(subject.uiState.value.isSubmitEnabled)
    }

    private fun loginViewModel(
        authenticateRepository: AuthenticateRepository = FakeAuthenticateRepository(),
    ) = LoginViewModel(
        validateCredentials = ValidateLoginCredentialsUseCase(),
        loginWithCredentials = LoginWithCredentialsUseCase(authenticateRepository),
    )

    private class FakeAuthenticateRepository(
        private val loginFailure: Throwable? = null,
    ) : AuthenticateRepository {

        var loginEmail: String? = null
            private set

        var loginPassword: String? = null
            private set

        override suspend fun loginUser(
            email: String,
            password: String,
        ) {
            loginFailure?.let { throw it }
            loginEmail = email
            loginPassword = password
        }

        override suspend fun loginUserWithQR(qrCode: String) = Unit

        override suspend fun logoutUser() = Unit

        override suspend fun forgotPassword(
            email: String,
            otp: String?,
        ) = Unit
    }
}
