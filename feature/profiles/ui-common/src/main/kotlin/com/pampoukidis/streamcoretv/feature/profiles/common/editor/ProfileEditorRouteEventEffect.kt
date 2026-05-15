package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError

@Composable
fun ProfileEditorRouteEventEffect(
    viewModel: ProfileEditorViewModel,
    onProfileSaved: () -> Unit,
    onClose: () -> Unit,
    onError: (AppError) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentProfileSaved by rememberUpdatedState(onProfileSaved)
    val currentClose by rememberUpdatedState(onClose)
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    ProfileEditorEffect.ProfileSaved -> currentProfileSaved()
                    ProfileEditorEffect.Close -> currentClose()
                    is ProfileEditorEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}


