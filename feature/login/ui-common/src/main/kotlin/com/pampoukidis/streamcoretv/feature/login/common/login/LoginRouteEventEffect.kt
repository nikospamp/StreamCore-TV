package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError

@Composable
fun LoginRouteEventEffect(
    viewModel: LoginViewModel,
    onLoginSucceeded: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
    onError: (AppError) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLoginSucceeded by rememberUpdatedState(onLoginSucceeded)
    val currentForgotPassword by rememberUpdatedState(onForgotPassword)
    val currentCreateAccount by rememberUpdatedState(onCreateAccount)
    val currentHelp by rememberUpdatedState(onHelp)
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    LoginEffect.LoginSucceeded -> currentLoginSucceeded()
                    LoginEffect.ForgotPassword -> currentForgotPassword()
                    LoginEffect.CreateAccount -> currentCreateAccount()
                    LoginEffect.Help -> currentHelp()
                    is LoginEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}
