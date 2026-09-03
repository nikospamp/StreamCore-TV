package com.pampoukidis.streamcoretv.feature.search.web.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchRouteEventEffect
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebSearchRoute(
    profileId: String,
    selectedContentKey: WebBrowseFocusKey?,
    onContentSelected: (ContentModel, WebBrowseFocusKey) -> Unit,
    onBack: () -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(SearchAction.Load(profileId))
    }

    SearchRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = { content ->
            onContentSelected(content, content.webSearchFocusKey())
        },
    )

    CompositionLocalProvider(LocalWebSearchBackHandler provides onBack) {
        WebSearchScreen(
            state = state,
            onAction = viewModel::onAction,
            selectedContentKey = selectedContentKey,
            returnFocusKey = returnFocusKey,
            onReturnFocusConsumed = onReturnFocusConsumed,
        )
    }
}
