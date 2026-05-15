package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreen
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorViewModel

@Composable
fun TvProfileEditorRoute(
    mode: ProfileEditorMode,
    profileId: String?,
    onProfileSaved: () -> Unit,
    onClose: () -> Unit,
    onError: (AppError) -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(mode, profileId, viewModel) {
        viewModel.onAction(
            ProfileEditorAction.Load(
                mode = mode,
                profileId = profileId,
            ),
        )
    }

    ProfileEditorRouteEventEffect(
        viewModel = viewModel,
        onProfileSaved = onProfileSaved,
        onClose = onClose,
        onError = onError,
    )

    ProfileEditorScreen(
        state = state,
        onAction = viewModel::onAction,
        useTvControls = true,
    )
}


