package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerRouteEventEffect
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerViewModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebPlayerRoute(
    request: PlaybackRequestModel,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val videoSurface by viewModel.videoSurface.collectAsStateWithLifecycle()
    val fullscreenController = rememberWebPlayerFullscreenController()

    LaunchedEffect(request, viewModel) {
        viewModel.onAction(PlayerAction.Load(request, isPipSupported = false))
    }

    WebPlayerDocumentVisibilityEffect { visible ->
        viewModel.onAction(PlayerAction.ForegroundChanged(visible))
    }

    DisposableEffect(fullscreenController) {
        onDispose {
            fullscreenController.exit()
        }
    }

    PlayerRouteEventEffect(
        viewModel = viewModel,
        onBack = onBack,
        onEnterPictureInPicture = {},
    )

    CompositionLocalProvider(
        LocalWebPlayerFullscreenController provides fullscreenController,
    ) {
        WebPlayerScreen(
            state = state,
            videoSurface = videoSurface,
            onAction = viewModel::onAction,
        )
    }
}
