package com.pampoukidis.streamcoretv.feature.login.common.contract

import com.pampoukidis.streamcoretv.feature.login.domain.LoginFieldError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val isSubmitEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
