package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebControlStyle
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
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
    val controls = StreamCoreWebControlStyle(StreamCoreControlDefaults.style())
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
        background = MaterialTheme.colorScheme.background,
        foreground = MaterialTheme.colorScheme.onSurface,
        border = if (errorMessage == null) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.error
        },
    )
    val errorStyle = webInputErrorStyle(MaterialTheme.colorScheme.error)
    val nameBackground = MaterialTheme.colorScheme.background.cssColor()
    val nameForeground = MaterialTheme.colorScheme.onSurfaceVariant.cssColor()
    val nameFocus = if (errorMessage == null) {
        MaterialTheme.colorScheme.primary.cssColor()
    } else {
        MaterialTheme.colorScheme.error.cssColor()
    }
    HtmlElementView(
        factory = {
            val input = (document.createElement("input") as HTMLInputElement).apply {
                type = "text"
                id = "streamcore-profile-name"
                setAttribute("aria-label", "Username")
                setAttribute("autocomplete", "nickname")
                setAttribute("data-testid", "profile-display-name")
                setAttribute("maxlength", "32")
                listeners.attach(this)
            }
            val error = (document.createElement("div") as HTMLElement).apply {
                id = PROFILE_DISPLAY_NAME_ERROR_ID
                setAttribute("data-testid", "profile-display-name-error")
                setAttribute("role", "alert")
                setAttribute("aria-live", "polite")
                hidden = true
            }
            (document.createElement("div") as HTMLElement).apply {
                setAttribute("data-testid", "profile-display-name-form")
                style.cssText = "position:relative;box-sizing:border-box;width:100%;height:100%;padding-top:12px;margin:0;display:flex;flex-direction:column;gap:6px;"
                appendChild((document.createElement("label") as HTMLElement).apply {
                    textContent = "Username"
                    setAttribute("for", "streamcore-profile-name")
                })
                appendChild(document.createElement("style"))
                appendChild(input)
                appendChild(error)
            }
        },
        update = { container ->
            val input = container.querySelector("[data-testid='profile-display-name']") as HTMLInputElement
            val error = container.querySelector("#$PROFILE_DISPLAY_NAME_ERROR_ID") as HTMLElement
            input.style.cssText = inputStyle + controls.input(enabled)
            (container.querySelector("label") as HTMLElement).style.cssText =
                "position:absolute;top:4px;left:12px;padding:0 4px;font:400 12px/16px system-ui,Segoe UI,Arial,sans-serif;" +
                    "background:" + nameBackground + ";color:" + nameForeground + ";"
            container.querySelector("style")?.textContent =
                "#streamcore-profile-name:focus{outline:none;border-color:" + nameFocus + "!important;box-shadow:inset 0 0 0 1px " + nameFocus + "}"
            if (input.value != value) input.value = value
            input.disabled = !enabled
            if (errorMessage == null) {
                input.removeAttribute("aria-invalid")
                input.removeAttribute("aria-errormessage")
                error.hidden = true
                error.textContent = ""
            } else {
                input.setAttribute("aria-invalid", "true")
                input.setAttribute("aria-errormessage", PROFILE_DISPLAY_NAME_ERROR_ID)
                error.style.cssText = errorStyle
                error.textContent = errorMessage
                error.hidden = false
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
        "border:1px solid ${border.cssColor()};" +
        "background:${background.cssColor()};color:${foreground.cssColor()};"
}

private fun webInputErrorStyle(color: Color): String {
    return "box-sizing:border-box;min-height:26px;color:${color.cssColor()};" +
        "font:500 14px system-ui,Segoe UI,Arial,sans-serif;line-height:20px;" +
        "white-space:normal;overflow-wrap:anywhere;"
}

private fun Color.cssColor(): String {
    val argb = toArgb()
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    return "rgb($red,$green,$blue)"
}

private const val PROFILE_DISPLAY_NAME_ERROR_ID = "streamcore-profile-display-name-error"
