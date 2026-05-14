package com.pampoukidis.streamcoretv.feature.login.common.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.feature.login.domain.LoginWithCredentialsUseCase
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginEvent
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.domain.LoginCredentials
import com.pampoukidis.streamcoretv.feature.login.domain.LoginValidationResult
import com.pampoukidis.streamcoretv.feature.login.domain.ValidateLoginCredentialsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validateCredentials: ValidateLoginCredentialsUseCase,
    private val loginWithCredentials: LoginWithCredentialsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    private var hasRequestedValidation = false

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> onCredentialsChanged(email = action.value)
            is LoginAction.PasswordChanged -> onCredentialsChanged(password = action.value)
            LoginAction.Submit -> submit()
            LoginAction.ForgotPassword -> emitEvent(LoginEvent.ForgotPassword)
            LoginAction.CreateAccount -> emitEvent(LoginEvent.CreateAccount)
            LoginAction.Help -> emitEvent(LoginEvent.Help)
            LoginAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun onCredentialsChanged(
        email: String = _uiState.value.email,
        password: String = _uiState.value.password,
    ) {
        val validation = validateCredentials(email = email, password = password)
        _uiState.update {
            it.copy(
                email = email,
                password = password,
                emailError = validation.emailError.takeIf { hasRequestedValidation },
                passwordError = validation.passwordError.takeIf { hasRequestedValidation },
                isSubmitEnabled = validation.isValid && !it.isLoading,
                errorMessage = null,
            )
        }
    }

    private fun submit() {
        if (_uiState.value.isLoading) return

        hasRequestedValidation = true
        val currentState = _uiState.value
        val validation = validateCredentials(
            email = currentState.email,
            password = currentState.password,
        )

        if (!validation.isValid) {
            showValidationErrors(validation)
            return
        }

        val credentials = LoginCredentials(
            email = currentState.email.trim(),
            password = currentState.password,
        )

        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                isSubmitEnabled = false,
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching { loginWithCredentials(credentials) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSubmitEnabled = true,
                        )
                    }
                    _events.emit(LoginEvent.LoginSucceeded)
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSubmitEnabled = true,
                            errorMessage = "",
                        )
                    }
                }
        }
    }

    private fun showValidationErrors(validation: LoginValidationResult) {
        _uiState.update {
            it.copy(
                emailError = validation.emailError,
                passwordError = validation.passwordError,
                isSubmitEnabled = false,
                isLoading = false,
            )
        }
    }

    private fun emitEvent(event: LoginEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}
