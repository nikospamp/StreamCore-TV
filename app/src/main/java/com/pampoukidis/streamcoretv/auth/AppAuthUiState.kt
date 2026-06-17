package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel

sealed interface AppAuthUiState {
    data object Loading : AppAuthUiState

    data class Ready(
        val authState: AuthStateModel,
    ) : AppAuthUiState
}