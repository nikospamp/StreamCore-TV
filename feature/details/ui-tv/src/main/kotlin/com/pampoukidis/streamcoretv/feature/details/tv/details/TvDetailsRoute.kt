package com.pampoukidis.streamcoretv.feature.details.tv.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRouteEventEffect
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest

@Composable
fun TvDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (String) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, contentId, viewModel) {
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
        state = state,
        onAction = viewModel::onAction,
    )
}
