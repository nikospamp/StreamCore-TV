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
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcoretv.feature.details.common.details.withInitialContent
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest

@Composable
fun TvDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (ContentModel) -> Unit,
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
        viewModel.onAction(
            DetailsAction.Load(
                request = DetailsRequest(
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
        onPlaySelected = {},
        onBack = onBack,
        onError = onError,
    )

    TvDetailsScreen(
        state = displayState,
        onAction = viewModel::onAction,
        sharedElementScope = sharedElementScope,
    )
}