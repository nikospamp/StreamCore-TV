package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebControlStyle
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebProfileEditorActionButton(
    target: WebProfileEditorActionTarget,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onDisplayNameCommit: (String) -> Unit,
    modifier: Modifier,
    requestFocus: Boolean,
) {
    val controls = StreamCoreWebControlStyle(StreamCoreControlDefaults.style())
    val currentEnabled = rememberUpdatedState(enabled)
    val currentOnClick = rememberUpdatedState(onClick)
    val currentCommit = rememberUpdatedState(onDisplayNameCommit)
    val action = when (target) {
        WebProfileEditorActionTarget.Cancel -> "cancel"
        WebProfileEditorActionTarget.Save -> "save"
        WebProfileEditorActionTarget.Delete -> "delete"
    }
    val listener = remember(target) {
        { event: Event ->
            event.preventDefault()
            if (currentEnabled.value) {
                if (target == WebProfileEditorActionTarget.Save) {
                    val value = (document.querySelector("[data-testid='profile-display-name']") as? HTMLInputElement)?.value
                    if (value != null) currentCommit.value(value)
                }
                document.body?.setAttribute("data-profile-editor-action", action)
                currentOnClick.value()
            }
        }
    }
    val transparent = MaterialTheme.colorScheme.transparentContainer
    val foreground = if (target == WebProfileEditorActionTarget.Delete) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            androidx.compose.runtime.withFrameNanos { }
            (document.querySelector("[data-testid='profile-editor-" + action + "']") as? HTMLButtonElement)?.focus()
        }
    }
    HtmlElementView(
        factory = {
            (document.createElement("div") as HTMLElement).apply {
                style.cssText = "width:100%;height:100%;display:flex;align-items:center"
                appendChild(document.createElement("style"))
                appendChild((document.createElement("button") as HTMLButtonElement).apply {
                    type = "button"
                    setAttribute("data-testid", "profile-editor-" + action)
                    addEventListener("click", listener)
                })
            }
        },
        update = { container ->
            val button = container.querySelector("button") as HTMLButtonElement
            button.textContent = if (target == WebProfileEditorActionTarget.Cancel) "" else text
            if (target == WebProfileEditorActionTarget.Cancel) {
                // Exact geometry from the shared ic_close_24 asset.
                val svg = document.createElementNS("http://www.w3.org/2000/svg", "svg")
                svg.setAttribute("viewBox", "0 0 24 24")
                svg.setAttribute("width", "24")
                svg.setAttribute("height", "24")
                svg.setAttribute("aria-hidden", "true")
                val path = document.createElementNS("http://www.w3.org/2000/svg", "path")
                path.setAttribute("d", "M18.3,5.71L12,12l6.3,6.29l-1.41,1.42L10.59,13.41L4.29,19.71L2.88,18.3L9.17,12L2.88,5.7L4.29,4.29L10.59,10.59L16.89,4.29z")
                path.setAttribute("fill", "currentColor")
                svg.appendChild(path)
                button.appendChild(svg)
            }
            button.setAttribute("aria-label", text)
            button.disabled = !enabled
            button.style.cssText = "box-sizing:border-box;width:100%;height:100%;display:grid;place-items:center;padding:0 12px;border:0;" +
                controls.button(transparent, foreground, enabled)
            container.querySelector("style")?.textContent =
                controls.buttonStates("[data-testid='profile-editor-" + action + "']")
        },
        onRelease = { container ->
            container.querySelector("button")?.removeEventListener("click", listener)
        },
        modifier = modifier,
    )
}
