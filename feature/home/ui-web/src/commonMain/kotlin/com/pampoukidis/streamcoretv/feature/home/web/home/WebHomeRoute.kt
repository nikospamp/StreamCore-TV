package com.pampoukidis.streamcoretv.feature.home.web.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeRouteEventEffect
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebHomeRoute(
    profileId: String,
    selectedContentKey: WebBrowseFocusKey?,
    onContentSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onError: (AppError) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(HomeAction.Load(profileId))
    }

    HomeRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = { content ->
            onContentSelected(content, content.toWebHomeFocusKey())
        },
        onError = onError,
    )

    WebHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        selectedContentKey = selectedContentKey,
        returnFocusKey = returnFocusKey,
        onReturnFocusConsumed = onReturnFocusConsumed,
    )
}
