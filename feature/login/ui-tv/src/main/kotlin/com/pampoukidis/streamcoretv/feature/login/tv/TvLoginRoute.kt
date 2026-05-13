package com.pampoukidis.streamcoretv.feature.login.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.feature.login.common.effects.LoginRouteEventEffect
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginViewModel

@Composable
fun TvLoginRoute(
    onLoginSucceeded: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoginRouteEventEffect(
        viewModel = viewModel,
        onLoginSucceeded = onLoginSucceeded,
        onForgotPassword = onForgotPassword,
        onCreateAccount = onCreateAccount,
        onHelp = onHelp,
    )

    TvLoginScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
