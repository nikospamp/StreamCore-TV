package com.pampoukidis.streamcoretv.feature.search.common.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

@Composable
fun SearchRouteEventEffect(
    viewModel: SearchViewModel,
    onContentSelected: (StreamCoreContent) -> Unit,
    onContentArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContentSelected by rememberUpdatedState(onContentSelected)
    val currentContentArtworkSelected by rememberUpdatedState(onContentArtworkSelected)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.ContentSelected -> {
                        val artworkSelected = currentContentArtworkSelected
                        if (artworkSelected != null) {
                            artworkSelected(effect.content, effect.sourceArtworkUrl)
                        } else {
                            currentContentSelected(effect.content)
                        }
                    }
                }
            }
        }
    }
}
