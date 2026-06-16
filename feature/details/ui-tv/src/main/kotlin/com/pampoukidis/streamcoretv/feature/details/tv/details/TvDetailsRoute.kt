package com.pampoukidis.streamcoretv.feature.details.tv.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRouteEventEffect
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcoretv.feature.details.common.details.withInitialContent
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import kotlinx.coroutines.delay

@Composable
fun TvDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (String) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
    initialContent: ContentModel? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayState = state.withInitialContent(
        contentId = contentId,
        initialContent = initialContent,
    )

    LaunchedEffect(profileId, contentId, viewModel) {
        if (initialContent?.id == contentId) {
            delay(InitialContentLoadDelayMillis)
        }
        viewModel.onAction(
            DetailsAction.Load(
                DetailsRequest(
                    profileId = profileId,
                    contentId = contentId,
                ),
            ),
        )
    }

    DetailsRouteEventEffect(
        viewModel = viewModel,
        onRecommendationSelected = onRecommendationSelected,
        onBack = onBack,
        onError = onError,
    )

    TvDetailsScreen(
        state = displayState,
        onAction = viewModel::onAction,
        sharedElementScope = sharedElementScope,
    )
}

private const val InitialContentLoadDelayMillis = 260L
