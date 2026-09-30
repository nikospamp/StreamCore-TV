package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest

@Composable
fun DetailsRouteEventEffect(
    viewModel: DetailsViewModel,
    onRecommendationSelected: (StreamCoreContent) -> Unit,
    onPlaySelected: (StreamCorePlaybackRequest) -> Unit,
    onBack: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    onRecommendationArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRecommendationSelected by rememberUpdatedState(onRecommendationSelected)
    val currentRecommendationArtworkSelected by rememberUpdatedState(onRecommendationArtworkSelected)
    val currentPlaySelected by rememberUpdatedState(onPlaySelected)
    val currentBack by rememberUpdatedState(onBack)
    val currentError by rememberUpdatedState(onError)
    val currentUriHandler by rememberUpdatedState(LocalUriHandler.current)

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is DetailsEffect.RecommendationSelected -> {
                        val artworkSelected = currentRecommendationArtworkSelected
                        if (artworkSelected != null) {
                            artworkSelected(effect.content, effect.sourceArtworkUrl)
                        } else {
                            currentRecommendationSelected(effect.content)
                        }
                    }
                    is DetailsEffect.PlaySelected -> currentPlaySelected(effect.request)
                    is DetailsEffect.OpenTrailer -> openDetailsTrailer(
                        trailer = effect.trailer,
                        uriHandler = currentUriHandler,
                        onError = currentError,
                    )
                    DetailsEffect.NavigateBack -> currentBack()
                    is DetailsEffect.ShowError -> currentError(effect.error)
                }
            }
        }
    }
}
