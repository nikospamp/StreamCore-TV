package com.pampoukidis.streamcoretv.feature.profiles.common.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.presentation.ProfilesViewModel

@Composable
fun ProfilesRouteEventEffect(
    viewModel: ProfilesViewModel,
    onProfileSelected: (ProfileModel) -> Unit,
    onError: (AppError) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentProfileSelected by rememberUpdatedState(onProfileSelected)
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ProfilesEffect.ProfileSelected -> currentProfileSelected(effect.profile)
                    is ProfilesEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}
