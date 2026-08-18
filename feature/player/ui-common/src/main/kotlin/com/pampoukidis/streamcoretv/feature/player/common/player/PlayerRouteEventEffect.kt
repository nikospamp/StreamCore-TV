package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun PlayerRouteEventEffect(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBack by rememberUpdatedState(onBack)
    val currentPip by rememberUpdatedState(onEnterPictureInPicture)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    PlayerEffect.NavigateBack -> currentBack()
                    PlayerEffect.EnterPictureInPicture -> currentPip()
                }
            }
        }
    }
}