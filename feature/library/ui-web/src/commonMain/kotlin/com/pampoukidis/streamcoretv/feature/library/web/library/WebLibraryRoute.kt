package com.pampoukidis.streamcoretv.feature.library.web.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryRouteEventEffect
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebLibraryRoute(
    profileId: String,
    selectedContentKey: WebBrowseFocusKey?,
    onContentSelected: (StreamCoreContent, WebBrowseFocusKey) -> Unit,
    onError: (StreamCoreError) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(key = libraryViewModelKey(profileId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(LibraryAction.Load(profileId))
    }

    LibraryRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = { content ->
            val focusKey = content.webLibraryFocusKey()
            if (focusKey == null) {
                onError(libraryFocusContractError())
            } else {
                onContentSelected(content, focusKey)
            }
        },
        onError = onError,
    )

    WebLibraryScreen(
        state = state,
        onAction = viewModel::onAction,
        selectedContentKey = selectedContentKey,
        returnFocusKey = returnFocusKey,
        onReturnFocusConsumed = onReturnFocusConsumed,
    )
}

private fun libraryFocusContractError(): StreamCoreError {
    return StreamCoreError.Unknown(
        source = StreamCoreErrorSource(
            operation = "library.selectContent",
            backendCode = "LIBRARY_SECTION_KEY_MISSING",
        ),
    )
}
