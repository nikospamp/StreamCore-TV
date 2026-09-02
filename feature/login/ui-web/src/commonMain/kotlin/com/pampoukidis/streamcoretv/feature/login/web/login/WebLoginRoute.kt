package com.pampoukidis.streamcoretv.feature.login.web.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginRouteEventEffect
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebLoginRoute(
    onLoginSucceeded: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
    onError: (AppError) -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoginRouteEventEffect(
        viewModel = viewModel,
        onLoginSucceeded = {
            viewModel.onAction(com.pampoukidis.streamcoretv.feature.login.common.login.LoginAction.IdentifierChanged(""))
            viewModel.onAction(com.pampoukidis.streamcoretv.feature.login.common.login.LoginAction.PasswordChanged(""))
            onLoginSucceeded()
        },
        onForgotPassword = onForgotPassword,
        onCreateAccount = onCreateAccount,
        onHelp = onHelp,
        onError = onError,
    )

    WebLoginScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
