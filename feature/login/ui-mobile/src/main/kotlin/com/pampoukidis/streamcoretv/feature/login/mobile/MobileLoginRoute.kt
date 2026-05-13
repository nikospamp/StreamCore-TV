package com.pampoukidis.streamcoretv.feature.login.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pampoukidis.streamcoretv.feature.login.common.effects.LoginRouteEventEffect
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginViewModel
import com.pampoukidis.streamcoretv.feature.login.domain.LoginCredentials

@Composable
fun MobileLoginRoute(
    onSubmitCredentials: (LoginCredentials) -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoginRouteEventEffect(
        viewModel = viewModel,
        onSubmitCredentials = onSubmitCredentials,
        onForgotPassword = onForgotPassword,
        onCreateAccount = onCreateAccount,
        onHelp = onHelp,
    )

    MobileLoginScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
