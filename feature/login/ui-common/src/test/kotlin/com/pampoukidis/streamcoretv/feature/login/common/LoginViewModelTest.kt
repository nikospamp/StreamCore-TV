package com.pampoukidis.streamcoretv.feature.login.common

import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginEvent
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginViewModel
import com.pampoukidis.streamcoretv.feature.login.domain.LoginFieldError
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
        val subject = LoginViewModel()

        subject.onAction(LoginAction.EmailChanged("lead@streamcore.tv"))
        subject.onAction(LoginAction.PasswordChanged("password"))

        assertTrue(subject.uiState.value.isSubmitEnabled)
        assertNull(subject.uiState.value.emailError)
        assertNull(subject.uiState.value.passwordError)
    }

    @Test
    fun `invalid submit surfaces validation errors`() {
        val subject = LoginViewModel()

        subject.onAction(LoginAction.Submit)

        val state = subject.uiState.value
        assertEquals(LoginFieldError.Required, state.emailError)
        assertEquals(LoginFieldError.Required, state.passwordError)
        assertFalse(state.isSubmitEnabled)
        assertFalse(state.isLoading)
    }

    @Test
    fun `submit emits trimmed credentials and marks loading until dispatched`() = runTest {
        val subject = LoginViewModel()
        val event = async { subject.events.first() }
        runCurrent()

        subject.onAction(LoginAction.EmailChanged(" lead@streamcore.tv "))
        subject.onAction(LoginAction.PasswordChanged("password"))
        subject.onAction(LoginAction.Submit)

        val submitEvent = event.await() as LoginEvent.SubmitCredentials
        assertEquals("lead@streamcore.tv", submitEvent.credentials.email)
        assertEquals("password", submitEvent.credentials.password)
        assertTrue(subject.uiState.value.isLoading)

        subject.onLoginRequestDispatched()

        assertFalse(subject.uiState.value.isLoading)
    }
}
