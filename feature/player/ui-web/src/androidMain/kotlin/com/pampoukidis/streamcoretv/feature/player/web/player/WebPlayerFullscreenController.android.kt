package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal actual fun rememberWebPlayerFullscreenController(): WebPlayerFullscreenController {
    return remember { AndroidWebPlayerFullscreenController }
}

@Composable
internal actual fun WebPlayerDocumentVisibilityEffect(
    onVisibilityChanged: (Boolean) -> Unit,
) {
    val currentOnVisibilityChanged = rememberUpdatedState(onVisibilityChanged)
    DisposableEffect(Unit) {
        currentOnVisibilityChanged.value(true)
        onDispose {
            currentOnVisibilityChanged.value(false)
        }
    }
}

@Composable
internal actual fun WebPlayerDocumentEscapeEffect(
    onEscape: () -> Unit,
) {
    return
}

@Composable
internal actual fun WebPlayerDocumentControlsRevealEffect(
    enabled: Boolean,
    onReveal: () -> Unit,
) {
    return
}

private object AndroidWebPlayerFullscreenController : WebPlayerFullscreenController {
    override val isFullscreen: State<Boolean> = mutableStateOf(false)

    override fun toggle() {
        return
    }

    override fun exit() {
        return
    }
}
