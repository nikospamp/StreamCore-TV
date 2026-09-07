package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/**
 * Per-control modifier hooks for [LoginForm], keeping platform focus and key handling in callers.
 * Defaults require no customization.
 *
 * The form appends sizing, autofill semantics, and test tags where applicable. Custom control
 * renderers must apply the supplied modifier to preserve these and the caller's behavior.
 */
@Immutable
data class LoginFormModifiers(
    val identifier: Modifier = Modifier,
    val password: Modifier = Modifier,
    val passwordVisibility: Modifier = Modifier,
    val submit: Modifier = Modifier,
    val forgotPassword: Modifier = Modifier,
    val createAccount: Modifier = Modifier,
    val help: Modifier = Modifier,
)
