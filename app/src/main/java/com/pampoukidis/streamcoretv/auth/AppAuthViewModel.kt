package com.pampoukidis.streamcoretv.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppAuthViewModel constructor(
    private val authenticateRepository: AuthenticateRepository,
) : ViewModel() {

    private val bootstrapCompleted = MutableStateFlow(false)
    private val activeProfileId = MutableStateFlow<String?>(null)
    private val logoutState = MutableStateFlow(LogoutState())
    private val effectsChannel = Channel<AppAuthEffect>(capacity = Channel.BUFFERED)

    val effects: Flow<AppAuthEffect> = effectsChannel.receiveAsFlow()

    val uiState: StateFlow<AppAuthUiState> = combine(
        bootstrapCompleted,
        authenticateRepository.authState,
        activeProfileId,
        logoutState,
    ) { isBootstrapCompleted, authState, activeProfileId, logoutState ->
        if (!isBootstrapCompleted) {
            return@combine AppAuthUiState.Loading
        }

        AppAuthUiState.Ready(
            authState = authState,
            activeProfileId = activeProfileId,
            isLogoutConfirmationVisible = logoutState.isConfirmationVisible,
            isLogoutInProgress = logoutState.isInProgress,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppAuthUiState.Loading,
        )

    init {
        bootstrapAuth()
    }

    fun onActiveProfileChanged(profileId: String?) {
        activeProfileId.value = profileId
    }

    fun onAction(action: AppAuthAction) {
        when (action) {
            AppAuthAction.RequestLogout -> requestLogout()
            AppAuthAction.DismissLogoutConfirmation -> dismissLogoutConfirmation()
            AppAuthAction.ConfirmLogout -> confirmLogout()
        }
    }

    private fun requestLogout() {
        val readyState = uiState.value as? AppAuthUiState.Ready ?: return
        if (readyState.authState !is AuthStateModel.LoggedIn) {
            return
        }

        logoutState.update { state ->
            if (state.isInProgress) state else state.copy(isConfirmationVisible = true)
        }
    }

    private fun dismissLogoutConfirmation() {
        logoutState.update { state ->
            if (state.isInProgress) state else state.copy(isConfirmationVisible = false)
        }
    }

    private fun confirmLogout() {
        val currentState = logoutState.value
        if (!currentState.isConfirmationVisible || currentState.isInProgress) {
            return
        }
        if (!logoutState.compareAndSet(currentState, currentState.copy(isInProgress = true))) {
            return
        }

        viewModelScope.launch {
            when (val result = logoutResult()) {
                is AppResult.Success -> {
                    activeProfileId.value = null
                    logoutState.value = LogoutState()
                }

                is AppResult.Failure -> {
                    if (result.error is AppError.Unauthorized || result.error is AppError.SessionExpired) {
                        activeProfileId.value = null
                        logoutState.value = LogoutState()
                    } else {
                        logoutState.update { state -> state.copy(isInProgress = false) }
                    }
                    effectsChannel.send(AppAuthEffect.ShowError(error = result.error))
                }
            }
        }
    }

    private suspend fun logoutResult(): AppResult<Unit> {
        return try {
            authenticateRepository.logoutUser()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            AppResult.Failure(
                AppError.Unknown(
                    source = ErrorSource(
                        operation = LOGOUT_OPERATION,
                    ),
                ),
            )
        }
    }

    private fun bootstrapAuth() {
        viewModelScope.launch {
            when (val result = bootstrapResult()) {
                is AppResult.Success -> {
                    bootstrapCompleted.value = true
                }

                is AppResult.Failure -> {
                    bootstrapCompleted.value = true
                    effectsChannel.send(AppAuthEffect.ShowError(error = result.error))
                }
            }
        }
    }

    private suspend fun bootstrapResult(): AppResult<AuthStateModel> {
        return try {
            authenticateRepository.bootstrapAuth()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            AppResult.Failure(
                AppError.Unknown(
                    source = ErrorSource(
                        operation = "bootstrapAuth",
                        backendCode = "AUTH_BOOTSTRAP_FAILURE",
                    ),
                ),
            )
        }
    }

    private data class LogoutState(
        val isConfirmationVisible: Boolean = false,
        val isInProgress: Boolean = false,
    )

    private companion object {
        const val LOGOUT_OPERATION = "logoutUser"
    }
}
