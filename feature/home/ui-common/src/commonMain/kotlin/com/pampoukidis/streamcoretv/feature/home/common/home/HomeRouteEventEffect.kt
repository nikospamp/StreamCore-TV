package com.pampoukidis.streamcoretv.feature.home.common.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

@Composable
fun HomeRouteEventEffect(
    viewModel: HomeViewModel,
    onContentSelected: (StreamCoreContent) -> Unit,
    onError: (StreamCoreError) -> Unit,
    onContentArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContentSelected by rememberUpdatedState(onContentSelected)
    val currentContentArtworkSelected by rememberUpdatedState(onContentArtworkSelected)
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is HomeEffect.ContentSelected -> {
                        val artworkSelected = currentContentArtworkSelected
                        if (artworkSelected != null) {
                            artworkSelected(effect.content, effect.sourceArtworkUrl)
                        } else {
                            currentContentSelected(effect.content)
                        }
                    }
                    is HomeEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}
