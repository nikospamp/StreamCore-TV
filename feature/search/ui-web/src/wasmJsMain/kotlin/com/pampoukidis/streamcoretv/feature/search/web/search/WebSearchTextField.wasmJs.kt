package com.pampoukidis.streamcoretv.feature.search.web.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebControlStyle
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.resources.stringResource
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import streamcoretv.feature.search.ui_web.generated.resources.Res
import streamcoretv.feature.search.ui_web.generated.resources.web_search_field_hint
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebSearchTextField(
    value: String,
    enabled: Boolean,
    requestFocus: Boolean,
    onValueChange: (String) -> Unit,
    onSubmitCommittedValue: (String) -> Unit,
    onEscape: () -> Unit,
    onFocusRequestConsumed: () -> Unit,
    modifier: Modifier,
) {
    val fieldHint = stringResource(Res.string.web_search_field_hint)
    val controlStyle = StreamCoreControlDefaults.style()
    val inputShape = remember(controlStyle.inputRadius) { RoundedCornerShape(controlStyle.inputRadius) }
    val controls = StreamCoreWebControlStyle(controlStyle)
    var focused by remember { mutableStateOf(false) }
    val currentCallbacks = rememberUpdatedState(
        WebSearchTextFieldCallbacks(
            enabled = enabled,
            onValueChange = onValueChange,
            onSubmitCommittedValue = onSubmitCommittedValue,
            onEscape = onEscape,
            onFocusChanged = { focused = it },
        ),
    )
    val listeners = remember {
        WebSearchTextFieldListeners(callbacks = { currentCallbacks.value })
    }
    val colors = WebSearchTextFieldColors(
        background = if (focused) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        foreground = MaterialTheme.colorScheme.onSurface,
        placeholder = MaterialTheme.colorScheme.onSurfaceVariant,
        outline = MaterialTheme.colorScheme.outline,
        focus = MaterialTheme.colorScheme.primary,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(StreamCoreWebDimens.ControlHeight)
            .clip(inputShape)
            .background(colors.background)
            .border(
                width = if (focused) StreamCoreDimens.Stroke.Default else StreamCoreDimens.Stroke.Thin,
                color = if (focused) colors.focus else colors.outline,
                shape = inputShape,
            )
            .padding(
                horizontal = StreamCoreDimens.Spacing.Medium,
                // Keep the HTML interop rectangle inside the Compose-painted focus border.
                vertical = StreamCoreDimens.Stroke.Default,
            ),
    ) {
        StreamCoreSearchIcon(color = colors.placeholder)
        HtmlElementView(
            factory = { createSearchFieldContainer(listeners, fieldHint) },
            update = { container ->
                val input = container.searchInput()
                listeners.updateValueFromState(input = input, value = value)
                input.disabled = !enabled
                input.style.cssText = fieldStyle(colors) + controls.input(enabled)
                container.searchStyle().textContent = focusStyle(colors)
                input.setAttribute("aria-label", fieldHint)
                input.setAttribute("placeholder", fieldHint)
                input.setAttribute("aria-disabled", (!enabled).toString())
            },
            onRelease = { container -> listeners.detach(container.searchInput()) },
            modifier = Modifier.weight(1f).height(StreamCoreWebDimens.ControlHeight),
        )
    }

    LaunchedEffect(requestFocus) {
        if (!requestFocus) {
            return@LaunchedEffect
        }
        repeat(FocusRequestAttempts) {
            withFrameNanos { }
            val input = document.querySelector(
                "[data-testid='${SearchTestTags.Field}']",
            ) as? HTMLInputElement
            input?.focus()
            if (input != null && document.activeElement == input) {
                onFocusRequestConsumed()
                return@LaunchedEffect
            }
        }
    }
}

private data class WebSearchTextFieldCallbacks(
    val enabled: Boolean,
    val onValueChange: (String) -> Unit,
    val onSubmitCommittedValue: (String) -> Unit,
    val onEscape: () -> Unit,
    val onFocusChanged: (Boolean) -> Unit,
)

@OptIn(ExperimentalWasmJsInterop::class)
private class WebSearchTextFieldListeners(
    private val callbacks: () -> WebSearchTextFieldCallbacks,
) {
    private var isComposing: Boolean = false
    private var lastCommittedValue: String? = null
    private var pendingPasteTimer: Int? = null

    private val focusListener: (Event) -> Unit = { callbacks().onFocusChanged(true) }
    private val blurListener: (Event) -> Unit = { callbacks().onFocusChanged(false) }

    private val inputListener: (Event) -> Unit = input@{ event ->
        val input = event.currentTarget as? HTMLInputElement ?: return@input
        if (!isComposing) {
            commitValue(input.value)
        }
    }
    private val changeListener: (Event) -> Unit = change@{ event ->
        val input = event.currentTarget as? HTMLInputElement ?: return@change
        commitValue(input.value)
    }
    private val compositionStartListener: (Event) -> Unit = {
        isComposing = true
    }
    private val compositionEndListener: (Event) -> Unit = compositionEnd@{ event ->
        isComposing = false
        val input = event.currentTarget as? HTMLInputElement ?: return@compositionEnd
        commitValue(input.value)
    }
    private val pasteListener: (Event) -> Unit = paste@{ event ->
        val input = event.currentTarget as? HTMLInputElement ?: return@paste
        pendingPasteTimer?.let { timerId -> window.clearTimeout(timerId) }
        pendingPasteTimer = window.setTimeout(
            {
                pendingPasteTimer = null
                if (!isComposing) {
                    commitValue(input.value)
                }
                null
            },
            0,
        )
    }
    private val keyDownListener: (Event) -> Unit = keyDown@{ event ->
        val keyboardEvent = event as? KeyboardEvent ?: return@keyDown
        when (keyboardEvent.key) {
            "Escape" -> {
                if (!isComposing) {
                    keyboardEvent.preventDefault()
                    callbacks().onEscape()
                }
            }
            "Enter" -> {
                val current = callbacks()
                if (!isComposing && current.enabled) {
                    keyboardEvent.preventDefault()
                    val input = keyboardEvent.currentTarget as? HTMLInputElement ?: return@keyDown
                    current.onSubmitCommittedValue(input.value)
                }
            }
        }
    }

    fun attach(input: HTMLInputElement) {
        input.addEventListener("focus", focusListener)
        input.addEventListener("blur", blurListener)
        input.addEventListener("input", inputListener)
        input.addEventListener("change", changeListener)
        input.addEventListener("compositionstart", compositionStartListener)
        input.addEventListener("compositionend", compositionEndListener)
        input.addEventListener("paste", pasteListener)
        input.addEventListener("keydown", keyDownListener)
    }

    fun detach(input: HTMLInputElement) {
        input.removeEventListener("focus", focusListener)
        input.removeEventListener("blur", blurListener)
        input.removeEventListener("input", inputListener)
        input.removeEventListener("change", changeListener)
        input.removeEventListener("compositionstart", compositionStartListener)
        input.removeEventListener("compositionend", compositionEndListener)
        input.removeEventListener("paste", pasteListener)
        input.removeEventListener("keydown", keyDownListener)
        pendingPasteTimer?.let { timerId -> window.clearTimeout(timerId) }
        pendingPasteTimer = null
        isComposing = false
        lastCommittedValue = null
    }

    fun updateValueFromState(input: HTMLInputElement, value: String) {
        if (!isComposing && input.value != value) {
            input.value = value
        }
        if (!isComposing) {
            lastCommittedValue = value
        }
    }

    private fun commitValue(value: String) {
        if (lastCommittedValue == value) {
            return
        }
        lastCommittedValue = value
        callbacks().onValueChange(value)
    }
}

private fun createSearchFieldContainer(
    listeners: WebSearchTextFieldListeners,
    fieldHint: String,
): HTMLElement {
    val input = (document.createElement("input") as HTMLInputElement).apply {
        type = "search"
        setAttribute("data-testid", SearchTestTags.Field)
        setAttribute("aria-label", fieldHint)
        setAttribute("placeholder", fieldHint)
        setAttribute("autocomplete", "off")
        setAttribute("autocapitalize", "none")
        setAttribute("enterkeyhint", "search")
        setAttribute("maxlength", SearchMaximumLength.toString())
        spellcheck = false
        listeners.attach(this)
    }
    return (document.createElement("div") as HTMLElement).apply {
        style.cssText = "box-sizing:border-box;width:100%;height:100%;margin:0;"
        appendChild((document.createElement("style") as HTMLElement).apply {
            setAttribute("data-search-field-style", "true")
        })
        appendChild(input)
    }
}

private fun HTMLElement.searchInput(): HTMLInputElement {
    return requireNotNull(querySelector("[data-testid='${SearchTestTags.Field}']") as? HTMLInputElement)
}

private fun HTMLElement.searchStyle(): HTMLElement {
    return requireNotNull(querySelector("[data-search-field-style='true']") as? HTMLElement)
}

private data class WebSearchTextFieldColors(
    val background: Color,
    val foreground: Color,
    val placeholder: Color,
    val outline: Color,
    val focus: Color,
)

private fun fieldStyle(colors: WebSearchTextFieldColors): String {
    return "box-sizing:border-box;width:100%;height:100%;" +
        "padding:0 ${StreamCoreWebDimens.HtmlInputPadding.value}px;" +
        "border:0;" +
        "background:transparent;color:${colors.foreground.cssColor()};" +
        "caret-color:${colors.focus.cssColor()};" +
        "outline:none;"
}

private fun focusStyle(colors: WebSearchTextFieldColors): String {
    return """
        [data-testid='${SearchTestTags.Field}']::placeholder {
            color: ${colors.placeholder.cssColor()};
            opacity: 1;
        }
        [data-testid='${SearchTestTags.Field}']:focus-visible {
            outline: none;
        }
        [data-testid='${SearchTestTags.Field}']:disabled {
            cursor: default;
        }
        @media (prefers-reduced-motion: reduce) {
            [data-testid='${SearchTestTags.Field}'] {
                scroll-behavior: auto !important;
            }
        }
    """.trimIndent()
}

private fun Color.cssColor(): String {
    val argb = toArgb()
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    return "rgb($red,$green,$blue)"
}

private const val SearchMaximumLength = 160
private const val FocusRequestAttempts = 4
