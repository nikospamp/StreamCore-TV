package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

internal interface WebPlayerFullscreenController {
    val isFullscreen: State<Boolean>

    fun toggle()
    fun exit()
}

internal val LocalWebPlayerFullscreenController = staticCompositionLocalOf<WebPlayerFullscreenController> {
    NoOpWebPlayerFullscreenController
}

@Composable
internal expect fun rememberWebPlayerFullscreenController(): WebPlayerFullscreenController

@Composable
internal expect fun WebPlayerDocumentVisibilityEffect(
    onVisibilityChanged: (Boolean) -> Unit,
)

@Composable
internal expect fun WebPlayerDocumentEscapeEffect(
    onEscape: () -> Unit,
)

@Composable
internal expect fun WebPlayerDocumentControlsRevealEffect(
    enabled: Boolean,
    onReveal: () -> Unit,
)

private object NoOpWebPlayerFullscreenController : WebPlayerFullscreenController {
    override val isFullscreen: State<Boolean> = mutableStateOf(false)

    override fun toggle() {
        return
    }

    override fun exit() {
        return
    }
}
