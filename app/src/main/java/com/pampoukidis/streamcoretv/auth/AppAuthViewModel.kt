package com.pampoukidis.streamcoretv.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppAuthViewModel @Inject constructor(
    authenticateRepository: AuthenticateRepository,
) : ViewModel() {

    val uiState: StateFlow<AppAuthUiState> = authenticateRepository.authState
        .map { authState ->
            val readyState: AppAuthUiState = AppAuthUiState.Ready(authState = authState)
            readyState
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppAuthUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}