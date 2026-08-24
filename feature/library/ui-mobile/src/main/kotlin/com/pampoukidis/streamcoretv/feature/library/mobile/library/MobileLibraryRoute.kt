package com.pampoukidis.streamcoretv.feature.library.mobile.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryRouteEventEffect
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryViewModel

@Composable
fun MobileLibraryRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onProfileSelected: () -> Unit,
    onError: (AppError) -> Unit,
    activeProfile: ProfileModel? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: LibraryViewModel = hiltViewModel(),
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

    MobileLibraryScreen(
        state = state,
        activeProfile = activeProfile,
        selectedContentKey = selectedContentKey,
        onAction = viewModel::onAction,
        onProfileSelected = onProfileSelected,
        sharedElementScope = sharedElementScope,
    )
}
