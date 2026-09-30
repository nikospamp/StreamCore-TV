package com.pampoukidis.streamcoretv.feature.details.web.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRouteEventEffect
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebDetailsRoute(
    profileId: String,
    contentId: String,
    onRecommendationSelected: (StreamCoreContent, WebBrowseFocusKey) -> Unit,
    onPlaySelected: (StreamCorePlaybackRequest, WebBrowseFocusKey) -> Unit,
    onBack: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    initialContent: StreamCoreContent? = null,
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

private fun detailsFocusContractError(): StreamCoreError {
    return StreamCoreError.Unknown(
        source = StreamCoreErrorSource(
            operation = "details.selectRecommendation",
            backendCode = "RECOMMENDATION_ID_MISSING",
        ),
    )
}
