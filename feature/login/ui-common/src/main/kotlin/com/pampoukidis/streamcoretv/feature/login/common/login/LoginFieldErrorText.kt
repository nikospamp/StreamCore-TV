package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError

@Composable
fun LoginFieldError.text(): String = when (this) {
    LoginFieldError.Required -> stringResource(R.string.login_email_required)
    LoginFieldError.InvalidEmail -> stringResource(R.string.login_email_invalid)
}

@Composable
fun LoginFieldError.passwordText(): String = when (this) {
    LoginFieldError.Required -> stringResource(R.string.login_password_required)
    LoginFieldError.InvalidEmail -> stringResource(R.string.login_password_required)
}
