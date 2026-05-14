package com.pampoukidis.streamcoretv.feature.login.common.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.login.domain.LoginWithCredentialsUseCase
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginEffect
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.domain.LoginCredentials
import com.pampoukidis.streamcoretv.feature.login.domain.LoginValidationResult
import com.pampoukidis.streamcoretv.feature.login.domain.ValidateLoginCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validateCredentials: ValidateLoginCredentialsUseCase,
    private val loginWithCredentials: LoginWithCredentialsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<LoginEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<LoginEffect> = effectsChannel.receiveAsFlow()

    private var hasRequestedValidation = false

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> onCredentialsChanged(email = action.value)
            is LoginAction.PasswordChanged -> onCredentialsChanged(password = action.value)
            LoginAction.Submit -> submit()
            LoginAction.ForgotPassword -> emitEffect(LoginEffect.ForgotPassword)
            LoginAction.CreateAccount -> emitEffect(LoginEffect.CreateAccount)
            LoginAction.Help -> emitEffect(LoginEffect.Help)
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
            )
        }

        viewModelScope.launch {
            when (val result = loginWithCredentials(credentials)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSubmitEnabled = true,
                        )
                    }
                    effectsChannel.send(LoginEffect.LoginSucceeded)
                }

                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSubmitEnabled = true,
                        )
                    }
                    effectsChannel.send(LoginEffect.ShowError(result.error))
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

    private fun emitEffect(effect: LoginEffect) {
        viewModelScope.launch {
            effectsChannel.send(effect)
        }
    }
}