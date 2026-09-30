package com.pampoukidis.streamcoretv.feature.profiles.web.pin

import androidx.compose.foundation.layout.height
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
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinFailure
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinTestTags
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinUiState
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebProfilePinInput(
    state: ProfilePinUiState,
    label: String,
    focusRequest: Int,
    onDraftChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onTab: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val currentChange = rememberUpdatedState(onDraftChanged)
    val currentCancel = rememberUpdatedState(onCancel)
    val currentTab = rememberUpdatedState(onTab)
    val inputListener: (Event) -> Unit = remember {
        { event ->
            (event.currentTarget as? HTMLInputElement)?.let { input ->
                val digits = input.value.filter { it in '0'..'9' }.take(input.maxLength.coerceAtLeast(0))
                input.value = digits
                currentChange.value(digits)
            }
        }
    }
    val keyListener: (Event) -> Unit = remember {
        { event ->
            val key = event as? KeyboardEvent
            if (key?.key == "Tab") {
                key.preventDefault()
                currentTab.value(key.shiftKey)
            }
        }
    }
    val escapeListener: (Event) -> Unit = remember {
        { event ->
            val key = event as? KeyboardEvent
            if (key?.key == "Escape") {
                key.preventDefault()
                key.stopImmediatePropagation()
                currentCancel.value()
            }
        }
    }
    val controls = StreamCoreWebControlStyle(StreamCoreControlDefaults.style())
    val colors = MaterialTheme.colorScheme
    val border = if (state.failure == ProfilePinFailure.Incorrect) colors.error else colors.outline
    HtmlElementView(
        factory = {
            (document.createElement("input") as HTMLInputElement).apply {
                type = "password"
                setAttribute("inputmode", "numeric")
                setAttribute("autocomplete", "off")
                setAttribute("data-testid", ProfilePinTestTags.Input)
                setAttribute("spellcheck", "false")
                addEventListener("input", inputListener)
                addEventListener("keydown", keyListener)
                document.addEventListener("keydown", escapeListener, true)
            }
        },
        update = { input ->
            input.setAttribute("aria-label", label)
            input.setAttribute("maxlength", state.digitCount.toString())
            input.setAttribute("aria-invalid", (state.failure == ProfilePinFailure.Incorrect).toString())
            input.style.cssText = "box-sizing:border-box;width:100%;height:100%;padding:0 16px;" +
                "font:500 24px system-ui;letter-spacing:8px;text-align:center;border:1px solid " + border.pinCss() + ";" +
                "background:" + colors.background.pinCss() + ";color:" + colors.onSurface.pinCss() + ";" + controls.input(state.inputEnabled)
            if (input.value != state.draft) input.value = state.draft
            input.disabled = !state.inputEnabled
            if (state.inputEnabled && input.getAttribute("data-focus-request") != focusRequest.toString()) {
                input.setAttribute("data-focus-request", focusRequest.toString())
                input.focus()
            }
        },
        onRelease = { input ->
            input.value = ""
            input.removeEventListener("input", inputListener)
            input.removeEventListener("keydown", keyListener)
            document.removeEventListener("keydown", escapeListener, true)
        },
        modifier = modifier.height(StreamCoreWebDimens.ControlHeight),
    )
}

private fun Color.pinCss(): String {
    val argb = toArgb()
    return "rgb(" + ((argb shr 16) and 0xff) + "," + ((argb shr 8) and 0xff) + "," + (argb and 0xff) + ")"
}
