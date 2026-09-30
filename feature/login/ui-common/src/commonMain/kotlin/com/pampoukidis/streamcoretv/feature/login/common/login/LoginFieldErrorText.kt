package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreLoginFieldError

@Composable
fun StreamCoreLoginFieldError.text(): String = when (this) {
    StreamCoreLoginFieldError.Required -> stringResource(Res.string.login_identifier_required)
}

@Composable
fun StreamCoreLoginFieldError.passwordText(): String = when (this) {
    StreamCoreLoginFieldError.Required -> stringResource(Res.string.login_password_required)
}
