package com.pampoukidis.streamcoretv.feature.library.tablet.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryRouteEventEffect
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryViewModel

@Composable
fun TabletLibraryRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onContentArtworkSelected: ((ContentModel, String?) -> Unit)? = null,
    onProfileSelected: () -> Unit,
    onError: (AppError) -> Unit,
    activeProfile: ProfileModel? = null,
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
