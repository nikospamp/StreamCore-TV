package com.pampoukidis.streamcoretv.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
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
    private val client: StreamCoreClient,
) : ViewModel() {

    private val sessionRestorationCompleted = MutableStateFlow(false)
    private val logoutState = MutableStateFlow(LogoutState())
    private val effectsChannel = Channel<AppAuthEffect>(capacity = Channel.BUFFERED)

    val effects: Flow<AppAuthEffect> = effectsChannel.receiveAsFlow()

    val uiState: StateFlow<AppAuthUiState> = combine(
        sessionRestorationCompleted,
        client.context,
        logoutState,
    ) { isSessionRestorationCompleted, context, logoutState ->
        if (!isSessionRestorationCompleted) {
            return@combine AppAuthUiState.Loading
        }

        AppAuthUiState.Ready(
            authState = context.account?.let { StreamCoreAuthState.LoggedIn(it) } ?: StreamCoreAuthState.LoggedOut,
            activeProfileId = context.profile?.id,
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
        restoreSession()
    }

    suspend fun clearProfileSelection(): StreamCoreResult<Unit> {
        return client.profiles.clearSelection()
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
        if (readyState.authState !is StreamCoreAuthState.LoggedIn) {
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
                is StreamCoreResult.Success -> {
                    logoutState.value = LogoutState()
                }

                is StreamCoreResult.Failure -> {
                    if (client.context.value.account == null) {
                        logoutState.value = LogoutState()
                    } else {
                        logoutState.update { state -> state.copy(isInProgress = false) }
                    }
                    effectsChannel.send(AppAuthEffect.ShowError(error = result.error))
                }
            }
        }
    }

    private suspend fun logoutResult(): StreamCoreResult<Unit> {
        return try {
            client.auth.logout()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            StreamCoreResult.Failure(
                StreamCoreError.Unknown(
                    source = StreamCoreErrorSource(
                        operation = LOGOUT_OPERATION,
                    ),
                ),
            )
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            when (val result = sessionRestorationResult()) {
                is StreamCoreResult.Success -> {
                    sessionRestorationCompleted.value = true
                }

                is StreamCoreResult.Failure -> {
                    sessionRestorationCompleted.value = true
                    effectsChannel.send(AppAuthEffect.ShowError(error = result.error))
                }
            }
        }
    }

    private suspend fun sessionRestorationResult(): StreamCoreResult<StreamCoreContext> {
        return try {
            client.auth.restoreSession()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            StreamCoreResult.Failure(
                StreamCoreError.Unknown(
                    source = StreamCoreErrorSource(
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
        const val LOGOUT_OPERATION = "logout"
    }
}
