package com.pampoukidis.streamcoretv.feature.profiles.tablet.editor

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorViewModel

@Composable
fun TabletProfileEditorRoute(
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

    TabletProfileEditorScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = Modifier.statusBarsPadding(),
    )
}

