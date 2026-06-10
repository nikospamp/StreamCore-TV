package com.pampoukidis.streamcoretv.feature.login.common.login

import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError

data class LoginUiState(
    val email: String = "nikos@mail.com",
    val password: String = "123456",
    val emailError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val isSubmitEnabled: Boolean = false,
    val isLoading: Boolean = false,
)
