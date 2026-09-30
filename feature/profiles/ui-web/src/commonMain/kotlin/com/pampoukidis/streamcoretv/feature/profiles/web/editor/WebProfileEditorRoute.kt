package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorViewModel
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebProfileEditorRoute(
    mode: ProfileEditorMode,
    profileId: String?,
    onProfileChanged: () -> Unit,
    onClose: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    viewModel: ProfileEditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(mode, profileId) {
        viewModel.onAction(ProfileEditorAction.Load(mode = mode, profileId = profileId))
    }

    ProfileEditorRouteEventEffect(
        viewModel = viewModel,
        onProfileSaved = onProfileChanged,
        onClose = onClose,
        onError = onError,
    )

    WebProfileEditorScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
