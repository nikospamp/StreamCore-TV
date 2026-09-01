package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError

@Composable
fun LoginFieldError.text(): String = when (this) {
    LoginFieldError.Required -> stringResource(Res.string.login_identifier_required)
}

@Composable
fun LoginFieldError.passwordText(): String = when (this) {
    LoginFieldError.Required -> stringResource(Res.string.login_password_required)
}
