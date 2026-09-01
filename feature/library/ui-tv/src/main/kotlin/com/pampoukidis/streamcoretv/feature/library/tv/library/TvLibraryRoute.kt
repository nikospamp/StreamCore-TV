package com.pampoukidis.streamcoretv.feature.library.tv.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryRouteEventEffect
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryViewModel

@Composable
fun TvLibraryRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onError: (AppError) -> Unit,
    returnFocusKey: String? = null,
    onReturnFocusConsumed: (String) -> Unit = {},
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(LibraryAction.Load(profileId))
    }

    LibraryRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = onContentSelected,
        onError = onError,
    )

    TvLibraryScreen(
        state = state,
        onAction = viewModel::onAction,
        selectedContentKey = selectedContentKey,
        returnFocusKey = returnFocusKey,
        onReturnFocusConsumed = onReturnFocusConsumed,
        sharedElementScope = sharedElementScope,
    )
}
