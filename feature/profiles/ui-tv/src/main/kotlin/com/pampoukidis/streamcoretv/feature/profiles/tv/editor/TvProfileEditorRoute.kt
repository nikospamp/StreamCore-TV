package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorViewModel

@Composable
fun TvProfileEditorRoute(
    mode: ProfileEditorMode,
    profileId: String?,
    onProfileSaved: () -> Unit,
    onClose: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    viewModel: ProfileEditorViewModel = koinViewModel(),
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

    TvProfileEditorScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

