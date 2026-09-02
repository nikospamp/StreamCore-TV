package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebProfileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    errorMessage: String?,
    modifier: Modifier,
) {
    val currentCallbacks = rememberUpdatedState(
        WebProfileNameFieldCallbacks(
            enabled = enabled,
            onValueChange = onValueChange,
            onSubmit = onSubmit,
        ),
    )
    val listeners = remember {
        WebProfileNameFieldListeners(callbacks = { currentCallbacks.value })
    }
    val inputStyle = webInputStyle(
        background = MaterialTheme.colorScheme.surface,
        foreground = MaterialTheme.colorScheme.onSurface,
        border = if (errorMessage == null) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.error
        },
    )
    HtmlElementView(
        factory = {
            val input = (document.createElement("input") as HTMLInputElement).apply {
                type = "text"
                setAttribute("aria-label", "Display name")
                setAttribute("autocomplete", "nickname")
                setAttribute("data-testid", "profile-display-name")
                setAttribute("maxlength", "32")
                setAttribute("form", PROFILE_EDITOR_FORM_ID)
                listeners.attach(this)
            }
            (document.createElement("div") as HTMLElement).apply {
                setAttribute("data-testid", "profile-display-name-form")
                style.cssText = "width:100%;height:100%;margin:0;"
                appendChild(input)
            }
        },
        update = { container ->
            val input = container.querySelector("[data-testid='profile-display-name']") as HTMLInputElement
            input.style.cssText = inputStyle
            if (input.value != value) input.value = value
            input.disabled = !enabled
            input.setAttribute("aria-invalid", (errorMessage != null).toString())
            if (errorMessage == null) {
                input.removeAttribute("aria-errormessage")
            } else {
                input.setAttribute("aria-errormessage", errorMessage)
            }
        },
        onRelease = { container ->
            val input = container.querySelector("[data-testid='profile-display-name']") as HTMLInputElement
            listeners.detach(input)
        },
        modifier = modifier,
    )
}

private data class WebProfileNameFieldCallbacks(
    val enabled: Boolean,
    val onValueChange: (String) -> Unit,
    val onSubmit: () -> Unit,
)

private class WebProfileNameFieldListeners(
    private val callbacks: () -> WebProfileNameFieldCallbacks,
) {
    private val inputListener: (Event) -> Unit = input@{ event ->
        val input = event.currentTarget as? HTMLInputElement ?: return@input
        callbacks().onValueChange(input.value)
    }
    private val keyDownListener: (Event) -> Unit = keyDown@{ event ->
        val keyboardEvent = event as? KeyboardEvent ?: return@keyDown
        if (keyboardEvent.key != "Enter") {
            return@keyDown
        }
        keyboardEvent.preventDefault()
        val current = callbacks()
        if (!current.enabled) {
            return@keyDown
        }
        val input = keyboardEvent.currentTarget as? HTMLInputElement ?: return@keyDown
        document.body?.setAttribute("data-profile-editor-action", "save")
        current.onValueChange(input.value)
        current.onSubmit()
    }

    fun attach(input: HTMLInputElement) {
        input.addEventListener("input", inputListener)
        input.addEventListener("keydown", keyDownListener)
    }

    fun detach(input: HTMLInputElement) {
        input.removeEventListener("input", inputListener)
        input.removeEventListener("keydown", keyDownListener)
    }
}

private fun webInputStyle(
    background: Color,
    foreground: Color,
    border: Color,
): String {
    return "box-sizing:border-box;width:100%;" +
        "height:${StreamCoreWebDimens.ControlHeight.value}px;" +
        "padding:0 ${StreamCoreWebDimens.HtmlInputPadding.value}px;" +
        "border:${StreamCoreWebDimens.FocusOuterBorder.value}px solid ${border.cssColor()};" +
        "border-radius:${StreamCoreWebDimens.HtmlInputRadius.value}px;" +
        "background:${background.cssColor()};color:${foreground.cssColor()};" +
        "font:400 ${StreamCoreWebDimens.HtmlInputFontSize.value}px system-ui,Segoe UI,Arial,sans-serif;"
}

private fun Color.cssColor(): String {
    val argb = toArgb()
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    return "rgb($red,$green,$blue)"
}
