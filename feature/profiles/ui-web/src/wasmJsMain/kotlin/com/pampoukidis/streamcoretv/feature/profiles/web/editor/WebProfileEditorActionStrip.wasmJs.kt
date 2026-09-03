package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebProfileEditorActionStrip(
    isSaving: Boolean,
    canDelete: Boolean,
    focusRequest: WebProfileEditorActionTarget?,
    onFocusRequestConsumed: () -> Unit,
    onMoveUp: () -> Unit,
    onDisplayNameCommit: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    val currentCallbacks = rememberUpdatedState(
        WebProfileEditorActionCallbacks(
            isSaving = isSaving,
            canDelete = canDelete,
            onMoveUp = onMoveUp,
            onDisplayNameCommit = onDisplayNameCommit,
            onCancel = onCancel,
            onSave = onSave,
            onDelete = onDelete,
        ),
    )
    val listeners = remember {
        WebProfileEditorActionListeners(callbacks = { currentCallbacks.value })
    }
    val primaryStyle = buttonStyle(
        background = MaterialTheme.colorScheme.primary,
        foreground = MaterialTheme.colorScheme.onPrimary,
    )
    val neutralStyle = buttonStyle(
        background = MaterialTheme.colorScheme.surfaceContainerHighest,
        foreground = MaterialTheme.colorScheme.onSurface,
    )
    val destructiveStyle = buttonStyle(
        background = MaterialTheme.colorScheme.errorContainer,
        foreground = MaterialTheme.colorScheme.onErrorContainer,
    )
    HtmlElementView(
        factory = {
            (document.createElement("form") as HTMLFormElement).apply {
                id = PROFILE_EDITOR_FORM_ID
                setAttribute("data-testid", "profile-editor-action-form")
                setAttribute("aria-label", "Profile editor actions")
                style.cssText = formStyle()
                appendChild(createButton("profile-editor-cancel", "Cancel", "button"))
                appendChild(createButton("profile-editor-save", "Save", "button"))
                appendChild(createButton("profile-editor-delete", "Delete", "button"))
                listeners.attach(this)
            }
        },
        update = { form ->
            val cancel = form.button("profile-editor-cancel")
            val save = form.button("profile-editor-save")
            val delete = form.button("profile-editor-delete")
            cancel.style.cssText = neutralStyle
            save.style.cssText = primaryStyle
            delete.style.cssText = destructiveStyle
            cancel.disabled = isSaving
            save.disabled = isSaving
            delete.disabled = isSaving
            delete.hidden = !canDelete
            save.textContent = if (isSaving) "Saving…" else "Save"
            form.setAttribute("aria-busy", isSaving.toString())
        },
        onRelease = { form ->
            listeners.detach(form)
        },
        modifier = modifier,
    )
    val currentOnFocusRequestConsumed = rememberUpdatedState(onFocusRequestConsumed)
    LaunchedEffect(focusRequest) {
        val target = when (focusRequest) {
            WebProfileEditorActionTarget.Cancel -> "profile-editor-cancel"
            WebProfileEditorActionTarget.Save -> "profile-editor-save"
            WebProfileEditorActionTarget.Delete -> "profile-editor-delete"
            null -> return@LaunchedEffect
        }
        androidx.compose.runtime.withFrameNanos { }
        val button = document.querySelector("[data-testid='$target']") as? HTMLButtonElement
            ?: return@LaunchedEffect
        button.scrollIntoView()
        button.focus()
        currentOnFocusRequestConsumed.value()
    }
}

private data class WebProfileEditorActionCallbacks(
    val isSaving: Boolean,
    val canDelete: Boolean,
    val onMoveUp: () -> Unit,
    val onDisplayNameCommit: (String) -> Unit,
    val onCancel: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
)

private class WebProfileEditorActionListeners(
    private val callbacks: () -> WebProfileEditorActionCallbacks,
) {
    private val cancelClickListener: (Event) -> Unit = { event ->
        event.preventDefault()
        recordProfileEditorAction("cancel")
        callbacks().onCancel()
    }
    private val saveClickListener: (Event) -> Unit = { event ->
        event.preventDefault()
        submitCurrentProfile()
    }
    private val deleteClickListener: (Event) -> Unit = { event ->
        event.preventDefault()
        recordProfileEditorAction("delete")
        callbacks().onDelete()
    }
    private val submitListener: (Event) -> Unit = { event ->
        event.preventDefault()
        submitCurrentProfile()
    }
    private val keyDownListener: (Event) -> Unit = keyDown@{ event ->
        val keyboardEvent = event as? KeyboardEvent ?: return@keyDown
        val target = keyboardEvent.target as? HTMLButtonElement
        val form = keyboardEvent.currentTarget as? HTMLFormElement ?: return@keyDown
        when (keyboardEvent.key) {
            "Escape" -> {
                keyboardEvent.preventDefault()
                recordProfileEditorAction("cancel")
                callbacks().onCancel()
            }
            "ArrowUp" -> {
                keyboardEvent.preventDefault()
                callbacks().onMoveUp()
            }
            "ArrowLeft" -> {
                keyboardEvent.preventDefault()
                when (target?.getAttribute("data-action")) {
                    "save" -> form.button("profile-editor-cancel").focus()
                    "delete" -> form.button("profile-editor-save").focus()
                }
            }
            "ArrowRight" -> {
                keyboardEvent.preventDefault()
                when (target?.getAttribute("data-action")) {
                    "cancel" -> form.button("profile-editor-save").focus()
                    "save" -> if (callbacks().canDelete) {
                        form.button("profile-editor-delete").focus()
                    }
                }
            }
        }
    }

    fun attach(form: HTMLFormElement) {
        form.button("profile-editor-cancel").addEventListener("click", cancelClickListener)
        form.button("profile-editor-save").addEventListener("click", saveClickListener)
        form.button("profile-editor-delete").addEventListener("click", deleteClickListener)
        form.addEventListener("submit", submitListener)
        form.addEventListener("keydown", keyDownListener)
    }

    fun detach(form: HTMLFormElement) {
        form.button("profile-editor-cancel").removeEventListener("click", cancelClickListener)
        form.button("profile-editor-save").removeEventListener("click", saveClickListener)
        form.button("profile-editor-delete").removeEventListener("click", deleteClickListener)
        form.removeEventListener("submit", submitListener)
        form.removeEventListener("keydown", keyDownListener)
    }

    private fun submitCurrentProfile() {
        val current = callbacks()
        if (current.isSaving) {
            return
        }
        recordProfileEditorAction("save")
        current.onDisplayNameCommit(currentDisplayName())
        current.onSave()
    }
}

private fun recordProfileEditorAction(action: String) {
    document.body?.setAttribute("data-profile-editor-action", action)
}

private fun currentDisplayName(): String {
    return (document.querySelector("[data-testid='profile-display-name']") as? HTMLInputElement)
        ?.value
        .orEmpty()
}

private fun createButton(testId: String, label: String, type: String): HTMLButtonElement {
    return (document.createElement("button") as HTMLButtonElement).apply {
        this.type = type
        textContent = label
        setAttribute("data-testid", testId)
        setAttribute("data-action", testId.substringAfterLast('-'))
        setAttribute("aria-label", label)
    }
}

private fun HTMLFormElement.button(testId: String): HTMLButtonElement {
    return requireNotNull(querySelector("[data-testid='$testId']") as? HTMLButtonElement)
}

private fun formStyle(): String {
    return "box-sizing:border-box;width:100%;height:100%;margin:0;display:flex;" +
        "justify-content:flex-end;align-items:center;gap:${StreamCoreWebDimens.ActionGap.value}px;"
}

private fun buttonStyle(background: Color, foreground: Color): String {
    return "box-sizing:border-box;min-height:${StreamCoreWebDimens.ControlHeight.value}px;" +
        "padding:0 ${StreamCoreWebDimens.ActionPadding.value}px;" +
        "border:${StreamCoreWebDimens.FocusOuterBorder.value}px solid transparent;" +
        "border-radius:${StreamCoreWebDimens.HtmlInputRadius.value}px;" +
        "background:${background.cssColor()};color:${foreground.cssColor()};" +
        "font:600 ${StreamCoreWebDimens.HtmlInputFontSize.value}px system-ui,Segoe UI,Arial,sans-serif;cursor:pointer;"
}

private fun Color.cssColor(): String {
    val argb = toArgb()
    return "rgb(${(argb shr 16) and 0xff},${(argb shr 8) and 0xff},${argb and 0xff})"
}

internal const val PROFILE_EDITOR_FORM_ID = "streamcore-profile-editor-form"
