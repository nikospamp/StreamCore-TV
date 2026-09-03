@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
internal actual fun rememberWebPlayerFullscreenController(): WebPlayerFullscreenController {
    val fullscreenState = remember {
        mutableStateOf(WebPlayerFullscreenInterop.isFullscreen())
    }
    val controller = remember(fullscreenState) {
        BrowserWebPlayerFullscreenController(fullscreenState)
    }

    DisposableEffect(controller) {
        val listener: (Event) -> Unit = {
            fullscreenState.value = WebPlayerFullscreenInterop.isFullscreen()
        }
        document.addEventListener(FullscreenChangeEvent, listener)
        onDispose {
            document.removeEventListener(FullscreenChangeEvent, listener)
        }
    }
    return controller
}

@Composable
internal actual fun WebPlayerDocumentVisibilityEffect(
    onVisibilityChanged: (Boolean) -> Unit,
) {
    val currentOnVisibilityChanged by rememberUpdatedState(onVisibilityChanged)
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = {
            currentOnVisibilityChanged(WebPlayerFullscreenInterop.isDocumentVisible())
        }
        currentOnVisibilityChanged(WebPlayerFullscreenInterop.isDocumentVisible())
        document.addEventListener(VisibilityChangeEvent, listener)
        onDispose {
            document.removeEventListener(VisibilityChangeEvent, listener)
            currentOnVisibilityChanged(false)
        }
    }
}

private class BrowserWebPlayerFullscreenController(
    private val fullscreenState: MutableState<Boolean>,
) : WebPlayerFullscreenController {
    override val isFullscreen: State<Boolean>
        get() {
            return fullscreenState
        }

    override fun toggle() {
        WebPlayerFullscreenInterop.toggleFullscreen()
    }

    override fun exit() {
        if (fullscreenState.value) {
            WebPlayerFullscreenInterop.exitFullscreen()
        }
    }
}

@JsModule("./web-player-fullscreen.mjs")
private external object WebPlayerFullscreenInterop {
    fun isFullscreen(): Boolean
    fun isDocumentVisible(): Boolean
    fun toggleFullscreen()
    fun exitFullscreen()
}

private const val FullscreenChangeEvent = "fullscreenchange"
private const val VisibilityChangeEvent = "visibilitychange"
