package com.pampoukidis.streamcoretv.feature.details.mobile.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRouteEventEffect
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcoretv.feature.details.common.details.withInitialContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreDetailsRequest
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest

@Composable
fun MobileDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (StreamCoreContent) -> Unit,
    onRecommendationArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
    onPlaySelected: (StreamCorePlaybackRequest) -> Unit,
    onBack: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    initialContent: StreamCoreContent? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: DetailsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayState = state.withInitialContent(
        contentId = contentId,
        initialContent = initialContent,
    )

    LaunchedEffect(profileId, contentId, viewModel) {
        viewModel.onAction(
            DetailsAction.Load(
                request = StreamCoreDetailsRequest(
                    profileId = profileId,
                    contentId = contentId,
                ),
                initialContent = initialContent,
            ),
        )
    }

    DetailsRouteEventEffect(
        viewModel = viewModel,
        onRecommendationSelected = onRecommendationSelected,
        onRecommendationArtworkSelected = onRecommendationArtworkSelected,
        onPlaySelected = onPlaySelected,
        onBack = onBack,
        onError = onError,
    )

    MobileDetailsScreen(
        state = displayState,
        onAction = viewModel::onAction,
        sharedElementScope = sharedElementScope,
    )
}
