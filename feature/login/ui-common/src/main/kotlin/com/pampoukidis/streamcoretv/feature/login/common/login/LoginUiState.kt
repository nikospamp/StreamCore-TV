package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError

data class LoginUiState(
    val identifier: String = "NikosPampoukidis",
    val password: String = "1234!@#\$qwer",
    val identifierError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val isSubmitEnabled: Boolean = false,
    val isLoading: Boolean = false,
)
