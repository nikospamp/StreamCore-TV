package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError

@Composable
fun DetailsRouteEventEffect(
    viewModel: DetailsViewModel,
    onRecommendationSelected: (String) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRecommendationSelected by rememberUpdatedState(onRecommendationSelected)
    val currentBack by rememberUpdatedState(onBack)
    val currentError by rememberUpdatedState(onError)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is DetailsEffect.RecommendationSelected -> {
                        currentRecommendationSelected(effect.contentId)
                    }
                    DetailsEffect.NavigateBack -> currentBack()
                    is DetailsEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}