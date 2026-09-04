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
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent

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
internal actual fun WebPlayerDocumentEscapeEffect(
    onEscape: () -> Unit,
) {
    val currentOnEscape by rememberUpdatedState(onEscape)
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            if (keyboardEvent?.key == EscapeKey) {
                keyboardEvent.preventDefault()
                keyboardEvent.stopImmediatePropagation()
                currentOnEscape()
            }
        }
        document.addEventListener(KeyDownEvent, listener, true)
        onDispose {
            document.removeEventListener(KeyDownEvent, listener, true)
        }
    }
}

@Composable
internal actual fun WebPlayerDocumentControlsRevealEffect(
    enabled: Boolean,
    onReveal: () -> Unit,
) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnReveal by rememberUpdatedState(onReveal)
    val keyCaptureState = remember { WebPlayerDocumentControlsRevealCaptureState() }
    val pointerCaptureState = remember { WebPlayerDocumentPointerRevealCaptureState() }
    pointerCaptureState.updateEnabled(enabled)
    DisposableEffect(keyCaptureState, pointerCaptureState) {
        var pendingPointerMoveFrameId: Int? = null
        val keyDownListener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            val key = keyboardEvent?.key
            val targetElement = keyboardEvent?.target as? Element
            val decision = if (keyboardEvent != null && key != null) {
                keyCaptureState.onKeyDown(
                    key = key,
                    repeat = keyboardEvent.repeat,
                    revealEnabled = currentEnabled,
                    revealTarget = isWebPlayerDocumentControlsRevealTarget(
                        tagName = targetElement?.tagName,
                        isContentEditable = (targetElement as? HTMLElement)?.isContentEditable == true,
                    ),
                )
            } else {
                WebPlayerDocumentKeyCaptureDecision.PassThrough
            }
            if (decision != WebPlayerDocumentKeyCaptureDecision.PassThrough) {
                keyboardEvent?.preventDefault()
                keyboardEvent?.stopImmediatePropagation()
                if (decision == WebPlayerDocumentKeyCaptureDecision.ConsumeAndReveal) {
                    currentOnReveal()
                }
            }
        }
        val keyUpListener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            val key = keyboardEvent?.key
            if (
                keyboardEvent != null &&
                key != null &&
                keyCaptureState.onKeyUp(key) == WebPlayerDocumentKeyCaptureDecision.Consume
            ) {
                keyboardEvent.preventDefault()
                keyboardEvent.stopImmediatePropagation()
            }
        }
        val pointerMoveListener: (Event) -> Unit = { event ->
            val pointerEvent = event as? PointerEvent
            val targetElement = pointerEvent?.target as? Element
            val revealTarget = isWebPlayerDocumentControlsRevealTarget(
                tagName = targetElement?.tagName,
                isContentEditable = (targetElement as? HTMLElement)?.isContentEditable == true,
            )
            if (
                pointerCaptureState.shouldScheduleMoveReveal(
                    revealEnabled = currentEnabled,
                    revealTarget = revealTarget,
                ) &&
                pendingPointerMoveFrameId == null
            ) {
                pendingPointerMoveFrameId = window.requestAnimationFrame {
                    pendingPointerMoveFrameId = null
                    if (pointerCaptureState.revealFromScheduledMove(currentEnabled)) {
                        currentOnReveal()
                    }
                }
            }
        }
        val pointerDownListener: (Event) -> Unit = { event ->
            val pointerEvent = event as? PointerEvent
            val targetElement = pointerEvent?.target as? Element
            val decision = if (pointerEvent != null) {
                pointerCaptureState.onPointerDown(
                    revealEnabled = currentEnabled,
                    revealTarget = isWebPlayerDocumentControlsRevealTarget(
                        tagName = targetElement?.tagName,
                        isContentEditable = (targetElement as? HTMLElement)?.isContentEditable == true,
                    ),
                    primaryButton = isWebPlayerPrimaryPointerButton(pointerEvent.button.toInt()),
                )
            } else {
                WebPlayerDocumentPointerCaptureDecision.PassThrough
            }
            if (decision != WebPlayerDocumentPointerCaptureDecision.PassThrough) {
                pendingPointerMoveFrameId?.let(window::cancelAnimationFrame)
                pendingPointerMoveFrameId = null
                pointerEvent?.preventDefault()
                pointerEvent?.stopImmediatePropagation()
                if (decision == WebPlayerDocumentPointerCaptureDecision.ConsumeAndReveal) {
                    currentOnReveal()
                }
            }
        }
        document.addEventListener(KeyDownEvent, keyDownListener, true)
        document.addEventListener(KeyUpEvent, keyUpListener, true)
        document.addEventListener(PointerMoveEvent, pointerMoveListener, true)
        document.addEventListener(PointerDownEvent, pointerDownListener, true)
        onDispose {
            pendingPointerMoveFrameId?.let(window::cancelAnimationFrame)
            pendingPointerMoveFrameId = null
            document.removeEventListener(KeyDownEvent, keyDownListener, true)
            document.removeEventListener(KeyUpEvent, keyUpListener, true)
            document.removeEventListener(PointerMoveEvent, pointerMoveListener, true)
            document.removeEventListener(PointerDownEvent, pointerDownListener, true)
        }
    }
}

internal enum class WebPlayerDocumentKeyCaptureDecision {
    PassThrough,
    Consume,
    ConsumeAndReveal,
}

internal class WebPlayerDocumentControlsRevealCaptureState {
    private var latchedKey: String? = null

    fun onKeyDown(
        key: String,
        repeat: Boolean,
        revealEnabled: Boolean,
        revealTarget: Boolean,
    ): WebPlayerDocumentKeyCaptureDecision {
        if (latchedKey == key) {
            return WebPlayerDocumentKeyCaptureDecision.Consume
        }
        if (repeat || !revealEnabled) {
            return WebPlayerDocumentKeyCaptureDecision.PassThrough
        }
        if (!isWebPlayerDocumentControlsRevealKey(key) || !revealTarget) {
            return WebPlayerDocumentKeyCaptureDecision.PassThrough
        }
        if (latchedKey != null) {
            return WebPlayerDocumentKeyCaptureDecision.Consume
        }
        latchedKey = key
        return WebPlayerDocumentKeyCaptureDecision.ConsumeAndReveal
    }

    fun onKeyUp(key: String): WebPlayerDocumentKeyCaptureDecision {
        if (latchedKey != key) {
            return WebPlayerDocumentKeyCaptureDecision.PassThrough
        }
        latchedKey = null
        return WebPlayerDocumentKeyCaptureDecision.Consume
    }
}

internal enum class WebPlayerDocumentPointerCaptureDecision {
    PassThrough,
    Consume,
    ConsumeAndReveal,
}

internal class WebPlayerDocumentPointerRevealCaptureState {
    private var enabled: Boolean = false
    private var revealDispatched: Boolean = false

    fun updateEnabled(enabled: Boolean) {
        if (enabled && !this.enabled) {
            revealDispatched = false
        }
        this.enabled = enabled
    }

    fun shouldScheduleMoveReveal(
        revealEnabled: Boolean,
        revealTarget: Boolean,
    ): Boolean {
        return revealEnabled && revealTarget && !revealDispatched
    }

    fun revealFromScheduledMove(revealEnabled: Boolean): Boolean {
        if (!revealEnabled || revealDispatched) {
            return false
        }
        revealDispatched = true
        return true
    }

    fun onPointerDown(
        revealEnabled: Boolean,
        revealTarget: Boolean,
        primaryButton: Boolean,
    ): WebPlayerDocumentPointerCaptureDecision {
        if (!revealEnabled || !revealTarget || !primaryButton) {
            return WebPlayerDocumentPointerCaptureDecision.PassThrough
        }
        if (revealDispatched) {
            return WebPlayerDocumentPointerCaptureDecision.Consume
        }
        revealDispatched = true
        return WebPlayerDocumentPointerCaptureDecision.ConsumeAndReveal
    }
}

internal fun isWebPlayerPrimaryPointerButton(button: Int): Boolean {
    return button == PrimaryPointerButton
}

internal fun isWebPlayerDocumentControlsRevealKey(key: String): Boolean {
    return key == ArrowLeftKey ||
        key == ArrowRightKey ||
        key == ArrowUpKey ||
        key == ArrowDownKey ||
        key == EnterKey ||
        key == SpaceKey ||
        key == LegacySpaceKey
}

internal fun isWebPlayerDocumentControlsRevealTarget(
    tagName: String?,
    isContentEditable: Boolean,
): Boolean {
    if (isContentEditable) {
        return false
    }
    return tagName?.equals(CanvasTagName, ignoreCase = true) == true ||
        tagName?.equals(BodyTagName, ignoreCase = true) == true ||
        tagName?.equals(ComposeHostTagName, ignoreCase = true) == true
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
private const val KeyDownEvent = "keydown"
private const val KeyUpEvent = "keyup"
private const val PointerMoveEvent = "pointermove"
private const val PointerDownEvent = "pointerdown"
private const val EscapeKey = "Escape"
private const val ArrowLeftKey = "ArrowLeft"
private const val ArrowRightKey = "ArrowRight"
private const val ArrowUpKey = "ArrowUp"
private const val ArrowDownKey = "ArrowDown"
private const val EnterKey = "Enter"
private const val SpaceKey = " "
private const val LegacySpaceKey = "Spacebar"
private const val CanvasTagName = "canvas"
private const val BodyTagName = "body"
private const val ComposeHostTagName = "div"
private const val PrimaryPointerButton = 0
