package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState

sealed interface AppAuthUiState {
    data object Loading : AppAuthUiState

    data class Ready(
        val authState: StreamCoreAuthState,
        val activeProfileId: String? = null,
        val isLogoutConfirmationVisible: Boolean = false,
        val isLogoutInProgress: Boolean = false,
    ) : AppAuthUiState
}
