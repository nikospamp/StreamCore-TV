package com.pampoukidis.streamcoretv.feature.library.tablet.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryRouteEventEffect
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryViewModel

@Composable
fun TabletLibraryRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (StreamCoreContent) -> Unit,
    onContentArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
    onProfileSelected: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    activeProfile: StreamCoreProfile? = null,
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
        onContentArtworkSelected = onContentArtworkSelected,
        onError = onError,
    )

    TabletLibraryScreen(
        state = state,
        activeProfile = activeProfile,
        selectedContentKey = selectedContentKey,
        onAction = viewModel::onAction,
        onProfileSelected = onProfileSelected,
        sharedElementScope = sharedElementScope,
    )
}
