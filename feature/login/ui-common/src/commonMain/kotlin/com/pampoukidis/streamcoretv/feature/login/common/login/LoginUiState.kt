package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginFieldError

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val identifierError: StreamCoreLoginFieldError? = null,
    val passwordError: StreamCoreLoginFieldError? = null,
    val isSubmitEnabled: Boolean = false,
    val isLoading: Boolean = false,
)
