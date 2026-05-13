package com.pampoukidis.streamcoretv.feature.login.common.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginViewModel
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginEvent

@Composable
fun LoginRouteEventEffect(
    viewModel: LoginViewModel,
    onLoginSucceeded: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLoginSucceeded by rememberUpdatedState(onLoginSucceeded)
    val currentForgotPassword by rememberUpdatedState(onForgotPassword)
    val currentCreateAccount by rememberUpdatedState(onCreateAccount)
    val currentHelp by rememberUpdatedState(onHelp)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    LoginEvent.LoginSucceeded -> currentLoginSucceeded()
                    LoginEvent.ForgotPassword -> currentForgotPassword()
                    LoginEvent.CreateAccount -> currentCreateAccount()
                    LoginEvent.Help -> currentHelp()
                }
            }
        }
    }
}
