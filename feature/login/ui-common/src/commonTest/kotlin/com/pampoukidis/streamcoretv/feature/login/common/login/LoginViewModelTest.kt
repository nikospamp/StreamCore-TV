package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcore.sdk.api.AuthService
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationIssue
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginFieldError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUpDispatcher() {
        mainDispatcherRule.setUp()
    }

    @AfterTest
    fun tearDownDispatcher() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `valid credentials enable submit`() {
        val subject = loginViewModel()

        subject.onAction(LoginAction.IdentifierChanged("lead@streamcore.tv"))
        subject.onAction(LoginAction.PasswordChanged("password"))

        assertTrue(subject.uiState.value.isSubmitEnabled)
        assertNull(subject.uiState.value.identifierError)
        assertNull(subject.uiState.value.passwordError)
    }

    @Test
    fun `invalid submit surfaces validation errors`() {
        val subject = loginViewModel()

        subject.onAction(LoginAction.Submit)

        val state = subject.uiState.value
        assertEquals(StreamCoreLoginFieldError.Required, state.identifierError)
        assertEquals(StreamCoreLoginFieldError.Required, state.passwordError)
        assertFalse(state.isSubmitEnabled)
        assertFalse(state.isLoading)
    }

    @Test
    fun `submit logs in with trimmed credentials and emits success`() = runTest {
        val repository = FakeAuthenticateRepository()
        val subject = loginViewModel(repository)
        val effect = async { subject.effects.first() }
        runCurrent()

        subject.onAction(LoginAction.IdentifierChanged(" lead@streamcore.tv "))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals("lead@streamcore.tv", repository.loginIdentifier)
        assertEquals("password", repository.loginPassword)
        assertEquals(LoginEffect.LoginSucceeded, effect.await())
        assertFalse(subject.uiState.value.isLoading)
        assertTrue(subject.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `submit failure emits error effect and re-enables submit`() = runTest {
        val error = StreamCoreError.Authentication()
        val subject = loginViewModel(
            authenticateRepository = FakeAuthenticateRepository(
                loginResult = StreamCoreResult.Failure(error),
            ),
        )
        val effect = async { subject.effects.first() }
        runCurrent()

        subject.onAction(LoginAction.IdentifierChanged("lead@streamcore.tv"))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals(LoginEffect.ShowError(error), effect.await())
        assertFalse(subject.uiState.value.isLoading)
        assertTrue(subject.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `SDK validation presents actionable field feedback`() = runTest {
        val subject = loginViewModel(FakeAuthenticateRepository(StreamCoreResult.Failure(
            StreamCoreError.Validation(listOf(StreamCoreValidationIssue(StreamCoreValidationField.Password, StreamCoreValidationReason.Required))),
        )))
        subject.onAction(LoginAction.IdentifierChanged("viewer"))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)
        runCurrent()

        assertEquals(StreamCoreLoginFieldError.Required, subject.uiState.value.passwordError)
        assertFalse(subject.uiState.value.isLoading)
        assertFalse(subject.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `effect emitted before collection is delivered to next collector`() = runTest {
        val subject = loginViewModel()

        subject.onAction(LoginAction.Help)
        runCurrent()

        assertEquals(LoginEffect.Help, subject.effects.first())
    }

    private fun loginViewModel(
        authenticateRepository: AuthService = FakeAuthenticateRepository(),
    ) = LoginViewModel(
        authenticateRepository = authenticateRepository,
    )

    private class FakeAuthenticateRepository(
        private val loginResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit),
    ) : AuthService {

        var loginIdentifier: String? = null
            private set

        var loginPassword: String? = null
            private set

        override suspend fun restoreSession(): StreamCoreResult<StreamCoreContext> {
            throw AssertionError("LoginViewModel must not restore a session during explicit login.")
        }

        override suspend fun login(
            identifier: String,
            password: String,
        ): StreamCoreResult<Unit> {
            loginIdentifier = identifier
            loginPassword = password
            return loginResult
        }

        override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> {
            return StreamCoreResult.Success(Unit)
        }

        override suspend fun logout(): StreamCoreResult<Unit> {
            return StreamCoreResult.Success(Unit)
        }

        override suspend fun recoverPassword(
            email: String,
            otp: String?,
        ): StreamCoreResult<Unit> = StreamCoreResult.Success(Unit)
    }
}
