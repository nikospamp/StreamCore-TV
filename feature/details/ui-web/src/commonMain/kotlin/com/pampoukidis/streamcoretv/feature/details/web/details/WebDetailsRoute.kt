package com.pampoukidis.streamcoretv.feature.details.web.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRouteEventEffect
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onPlaySelected: (PlaybackRequestModel, WebBrowseFocusKey) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
    initialContent: ContentModel? = null,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: DetailsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayState = state.webDetailsDisplayState(
        contentId = contentId,
        initialContent = initialContent,
    )

    LaunchedEffect(profileId, contentId, viewModel) {
        viewModel.onAction(
            webDetailsLoadAction(
                profileId = profileId,
                contentId = contentId,
                initialContent = initialContent,
            ),
        )
    }

    DetailsRouteEventEffect(
        viewModel = viewModel,
        onRecommendationSelected = { content ->
            val focusKey = content.webDetailsRecommendationFocusKey()
            if (focusKey == null) {
                onError(detailsFocusContractError())
            } else {
                onRecommendationSelected(content, focusKey)
            }
        },
        onPlaySelected = { request ->
            onPlaySelected(
                request,
                webDetailsActionFocusKey(WebDetailsPlayItem),
            )
        },
        onBack = onBack,
        onError = onError,
    )

    WebDetailsScreen(
        state = displayState,
        onAction = viewModel::onAction,
        returnFocusKey = returnFocusKey,
        onReturnFocusConsumed = onReturnFocusConsumed,
    )
}

private fun detailsFocusContractError(): AppError {
    return AppError.Unknown(
        source = ErrorSource(
            operation = "details.selectRecommendation",
            backendCode = "RECOMMENDATION_ID_MISSING",
        ),
    )
}
