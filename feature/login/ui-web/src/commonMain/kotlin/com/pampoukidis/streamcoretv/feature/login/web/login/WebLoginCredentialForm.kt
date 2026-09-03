package com.pampoukidis.streamcoretv.feature.login.web.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@Composable
internal expect fun WebLoginCredentialForm(
    identifier: String,
    password: String,
    identifierError: String?,
    passwordError: String?,
    isSubmitEnabled: Boolean,
    isLoading: Boolean,
    onIdentifierChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    identifierLabel: String,
    passwordLabel: String,
    showPasswordLabel: String,
    hidePasswordLabel: String,
    submitLabel: String,
    identifierFocus: FocusRequester,
    passwordFocus: FocusRequester,
    submitFocus: FocusRequester,
    modifier: Modifier = Modifier,
)
