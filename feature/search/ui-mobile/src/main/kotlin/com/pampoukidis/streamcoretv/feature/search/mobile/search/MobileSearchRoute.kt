package com.pampoukidis.streamcoretv.feature.search.mobile.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchRouteEventEffect
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchViewModel

@Composable
fun MobileSearchRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onContentArtworkSelected: ((ContentModel, String?) -> Unit)? = null,
    onBack: () -> Unit,
    bottomContentPadding: Dp,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var initialFocusRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(SearchAction.Load(profileId))
    }

    LaunchedEffect(state.resultQuery) {
        if (state.resultQuery != null) {
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(focusRequester, keyboardController) {
        if (!initialFocusRequested) {
            withFrameNanos { }
            focusRequester.requestFocus()
            keyboardController?.show()
            initialFocusRequested = true
        }
    }

    BackHandler {
        if (isImeVisible) {
            keyboardController?.hide()
        } else {
            onBack()
        }
    }

    SearchRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = { content ->
            keyboardController?.hide()
            onContentSelected(content)
        },
        onContentArtworkSelected = onContentArtworkSelected?.let { artworkSelected ->
            { content, sourceArtworkUrl ->
                keyboardController?.hide()
                artworkSelected(content, sourceArtworkUrl)
            }
        },
    )

    MobileSearchScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            if (action is SearchAction.SubmitQuery || action is SearchAction.RecentSelected) {
                keyboardController?.hide()
            }
        },
        gridState = gridState,
        bottomContentPadding = bottomContentPadding,
        focusRequester = focusRequester,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
    )
}
