package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

@Composable
fun ProfilesRouteEventEffect(
    viewModel: ProfilesViewModel,
    onProfileSelected: (StreamCoreProfile) -> Unit,
    onError: (StreamCoreError) -> Unit,
    onEntryStarted: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentProfileSelected by rememberUpdatedState(onProfileSelected)
    val currentError by rememberUpdatedState(onError)
    val currentEntryStarted by rememberUpdatedState(onEntryStarted)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    ProfilesEffect.EntryStarted -> currentEntryStarted()
                    is ProfilesEffect.ProfileSelected -> currentProfileSelected(effect.profile)
                    is ProfilesEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}

