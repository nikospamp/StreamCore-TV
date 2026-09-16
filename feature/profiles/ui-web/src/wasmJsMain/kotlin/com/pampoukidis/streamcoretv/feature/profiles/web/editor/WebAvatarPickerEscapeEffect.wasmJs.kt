package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.document
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@Composable
internal actual fun WebAvatarPickerEscapeEffect(onDismissRequest: () -> Unit) {
    val currentOnDismiss = rememberUpdatedState(onDismissRequest)
    DisposableEffect(Unit) {
        var dismissalRequested = false
        val listener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            if (keyboardEvent?.key == "Escape") {
                keyboardEvent.preventDefault()
                keyboardEvent.stopImmediatePropagation()
                if (!dismissalRequested) {
                    dismissalRequested = true
                    currentOnDismiss.value()
                }
            }
        }
        document.addEventListener("keydown", listener, true)
        onDispose {
            document.removeEventListener("keydown", listener, true)
        }
    }
}
