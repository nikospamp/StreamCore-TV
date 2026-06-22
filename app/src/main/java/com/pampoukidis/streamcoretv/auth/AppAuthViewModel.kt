package com.pampoukidis.streamcoretv.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppAuthViewModel @Inject constructor(
    private val authenticateRepository: AuthenticateRepository,
) : ViewModel() {

    private val bootstrapCompleted = MutableStateFlow(false)
    private val effectsChannel = Channel<AppAuthEffect>(capacity = Channel.BUFFERED)

    val effects: Flow<AppAuthEffect> = effectsChannel.receiveAsFlow()

    val uiState: StateFlow<AppAuthUiState> = combine(
        bootstrapCompleted,
        authenticateRepository.authState,
    ) { isBootstrapCompleted, authState ->
        if (!isBootstrapCompleted) {
            return@combine AppAuthUiState.Loading
        }

        AppAuthUiState.Ready(authState = authState)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppAuthUiState.Loading,
        )

    init {
        bootstrapAuth()
    }

    private fun bootstrapAuth() {
        viewModelScope.launch {
            when (val result = authenticateRepository.bootstrapAuth()) {
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

}