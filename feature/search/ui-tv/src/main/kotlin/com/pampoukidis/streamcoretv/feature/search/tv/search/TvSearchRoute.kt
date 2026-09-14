package com.pampoukidis.streamcoretv.feature.search.tv.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchRouteEventEffect
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchViewModel

@Composable
fun TvSearchRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onContentArtworkSelected: ((ContentModel, String?) -> Unit)? = null,
    onBack: () -> Unit,
    returnFocusKey: String? = null,
    onReturnFocusConsumed: (String) -> Unit = {},
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(SearchAction.Load(profileId))
        if (returnFocusKey == null) {
            withFrameNanos { }
            fieldFocusRequester.requestFocus()
        }
    }

    BackHandler {
        if (isKeyboardVisible) {
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

    TvSearchScreen(
        state = state,
        onAction = viewModel::onAction,
        onKeyboardRequested = { keyboardController?.show() },
        fieldFocusRequester = fieldFocusRequester,
        selectedContentKey = selectedContentKey,
        returnFocusKey = returnFocusKey,
        onReturnFocusConsumed = onReturnFocusConsumed,
        sharedElementScope = sharedElementScope,
    )
}
