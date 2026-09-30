package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginFieldError
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginCredentials
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginValidationResult
import com.pampoukidis.streamcore.sdk.api.AuthService
import com.pampoukidis.streamcore.sdk.api.validation.LoginValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel constructor(
    private val authenticateRepository: AuthService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<LoginEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<LoginEffect> = effectsChannel.receiveAsFlow()

    private var hasRequestedValidation = false

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.IdentifierChanged -> onCredentialsChanged(identifier = action.value)
            is LoginAction.PasswordChanged -> onCredentialsChanged(password = action.value)
            LoginAction.Submit -> submit()
            LoginAction.ForgotPassword -> emitEffect(LoginEffect.ForgotPassword)
            LoginAction.CreateAccount -> emitEffect(LoginEffect.CreateAccount)
            LoginAction.Help -> emitEffect(LoginEffect.Help)
        }
    }

    private fun onCredentialsChanged(
        identifier: String = _uiState.value.identifier,
        password: String = _uiState.value.password,
    ) {
        val validation = LoginValidator.validate(identifier = identifier, password = password)
        _uiState.update {
            it.copy(
                identifier = identifier,
                password = password,
                identifierError = validation.identifierError.takeIf { hasRequestedValidation },
                passwordError = validation.passwordError.takeIf { hasRequestedValidation },
                isSubmitEnabled = validation.isValid && !it.isLoading,
            )
        }
    }

    private fun submit() {
        if (_uiState.value.isLoading) {
            return
        }

        hasRequestedValidation = true

        val currentState = _uiState.value
        val validation = LoginValidator.validate(
            identifier = currentState.identifier,
            password = currentState.password,
        )

        if (!validation.isValid) {
            showValidationErrors(validation)
            return
        }

        val credentials = StreamCoreLoginCredentials(
            identifier = currentState.identifier.trim(),
            password = currentState.password,
        )

        _uiState.update {
            it.copy(
                identifierError = null,
                passwordError = null,
                isSubmitEnabled = false,
                isLoading = true,
            )
        }

        viewModelScope.launch {
            when (val result = authenticateRepository.login(credentials.identifier, credentials.password)) {
                is StreamCoreResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSubmitEnabled = true,
                        )
                    }
                    effectsChannel.send(LoginEffect.LoginSucceeded)
                }

                is StreamCoreResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isSubmitEnabled = true,
                        )
                    }
                    if (!showSdkValidation(result.error)) {
                        effectsChannel.send(LoginEffect.ShowError(result.error))
                    }
                }
            }
        }
    }

    private fun showSdkValidation(error: StreamCoreError): Boolean {
        val issues = (error as? StreamCoreError.Validation)?.issues ?: return false
        val validation = StreamCoreLoginValidationResult(
            identifierError = StreamCoreLoginFieldError.Required.takeIf {
                issues.any { it.field == StreamCoreValidationField.Identifier && it.reason == StreamCoreValidationReason.Required }
            },
            passwordError = StreamCoreLoginFieldError.Required.takeIf {
                issues.any { it.field == StreamCoreValidationField.Password && it.reason == StreamCoreValidationReason.Required }
            },
        )
        if (validation.isValid) return false
        showValidationErrors(validation)
        return true
    }

    private fun showValidationErrors(validation: StreamCoreLoginValidationResult) {
        _uiState.update {
            it.copy(
                identifierError = validation.identifierError,
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
