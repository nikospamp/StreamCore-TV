package com.pampoukidis.streamcoretv.feature.search.common.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel

@Composable
fun SearchRouteEventEffect(
    viewModel: SearchViewModel,
    onContentSelected: (ContentModel) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContentSelected by rememberUpdatedState(onContentSelected)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.ContentSelected -> currentContentSelected(effect.content)
                }
            }
        }
    }
}
