package com.pampoukidis.streamcoretv.feature.home.common.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError

@Composable
fun HomeRouteEventEffect(
    viewModel: HomeViewModel,
    onError: (AppError) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is HomeEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}