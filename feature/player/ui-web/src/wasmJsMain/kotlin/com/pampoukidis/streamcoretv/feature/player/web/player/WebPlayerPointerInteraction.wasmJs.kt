package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.webPlayerPointerInteraction(
    onInteraction: () -> Unit,
): Modifier {
    return onPointerEvent(PointerEventType.Enter) {
        onInteraction()
    }.onPointerEvent(PointerEventType.Move) {
        onInteraction()
    }
}
