package com.pampoukidis.streamcoretv.feature.library.common.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError

@Composable
fun LibraryRouteEventEffect(
    viewModel: LibraryViewModel,
    onContentSelected: (ContentModel) -> Unit,
    onError: (AppError) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContentSelected by rememberUpdatedState(onContentSelected)
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is LibraryEffect.ContentSelected -> currentContentSelected(effect.content)
                    is LibraryEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}
