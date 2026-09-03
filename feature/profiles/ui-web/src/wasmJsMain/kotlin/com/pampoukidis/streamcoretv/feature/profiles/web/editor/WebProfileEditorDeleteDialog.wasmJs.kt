package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDialogElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebProfileEditorDeleteDialog(
    profile: ProfileModel,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currentCallbacks = rememberUpdatedState(
        WebDeleteDialogCallbacks(
            isSaving = isSaving,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        ),
    )
    val listeners = remember {
        WebDeleteDialogListeners(callbacks = { currentCallbacks.value })
    }
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val neutral = MaterialTheme.colorScheme.surfaceContainerHighest
    val error = MaterialTheme.colorScheme.errorContainer
    val onError = MaterialTheme.colorScheme.onErrorContainer

    HtmlElementView(
        factory = {
            (document.createElement("dialog") as HTMLDialogElement).apply {
                id = PROFILE_EDITOR_DELETE_DIALOG_ID
                setAttribute("data-testid", "profile-editor-delete-dialog")
                setAttribute("aria-labelledby", PROFILE_EDITOR_DELETE_TITLE_ID)
                setAttribute("aria-describedby", PROFILE_EDITOR_DELETE_DESCRIPTION_ID)
                setAttribute("aria-modal", "true")
                val backdropStyle = document.createElement("style").apply {
                    textContent = "#$PROFILE_EDITOR_DELETE_DIALOG_ID::backdrop{background:rgba(0,0,0,.72)}"
                }
                val title = document.createElement("h2").apply {
                    id = PROFILE_EDITOR_DELETE_TITLE_ID
                }
                val description = document.createElement("p").apply {
                    id = PROFILE_EDITOR_DELETE_DESCRIPTION_ID
                    textContent = "This action cannot be undone."
                }
                val actions = (document.createElement("div") as HTMLElement).apply {
                    setAttribute("data-dialog-actions", "true")
                    appendChild(createDialogButton("profile-editor-delete-cancel", "Cancel"))
                    appendChild(createDialogButton("profile-editor-delete-confirm", "Delete"))
                }
                appendChild(backdropStyle)
                appendChild(title)
                appendChild(description)
                appendChild(actions)
                listeners.attach(this)
            }
        },
        update = { dialog ->
            dialog.style.cssText = dialogStyle(surface, onSurface)
            dialog.setAttribute("aria-busy", isSaving.toString())
            dialog.titleElement().apply {
                textContent = "Delete ${profile.displayName}?"
                style.cssText = "margin:0;font:600 28px system-ui,Segoe UI,Arial,sans-serif;"
            }
            dialog.descriptionElement().style.cssText =
                "margin:0;color:${muted.cssColor()};font:400 18px system-ui,Segoe UI,Arial,sans-serif;"
            val actions = dialog.querySelector("[data-dialog-actions='true']") as HTMLElement
            actions.style.cssText = "display:flex;justify-content:flex-end;gap:${StreamCoreWebDimens.ActionGap.value}px;"
            dialog.cancelButton().apply {
                disabled = isSaving
                style.cssText = dialogButtonStyle(neutral, onSurface)
            }
            dialog.confirmButton().apply {
                disabled = isSaving
                textContent = if (isSaving) "Deleting…" else "Delete"
                style.cssText = dialogButtonStyle(error, onError)
            }
        },
        onRelease = { dialog ->
            if (dialog.open) {
                dialog.close()
            }
            listeners.detach(dialog)
        },
    )

    LaunchedEffect(profile.id) {
        androidx.compose.runtime.withFrameNanos { }
        val dialog = document.getElementById(PROFILE_EDITOR_DELETE_DIALOG_ID) as? HTMLDialogElement
            ?: return@LaunchedEffect
        if (!dialog.open) {
            dialog.showModal()
        }
        dialog.cancelButton().focus()
    }
}

private data class WebDeleteDialogCallbacks(
    val isSaving: Boolean,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

private class WebDeleteDialogListeners(
    private val callbacks: () -> WebDeleteDialogCallbacks,
) {
    private val cancelClickListener: (Event) -> Unit = { event ->
        event.preventDefault()
        dismiss(event.currentTarget as? HTMLButtonElement)
    }
    private val confirmClickListener: (Event) -> Unit = { event ->
        event.preventDefault()
        val current = callbacks()
        if (!current.isSaving) {
            current.onConfirm()
        }
    }
    private val cancelListener: (Event) -> Unit = { event ->
        event.preventDefault()
        val dialog = event.currentTarget as? HTMLDialogElement
        val current = callbacks()
        if (!current.isSaving && dialog != null) {
            dialog.close()
            restoreDeleteFocus()
            current.onDismiss()
        }
    }
    private val keyDownListener: (Event) -> Unit = keyDown@{ event ->
        val keyboardEvent = event as? KeyboardEvent ?: return@keyDown
        val dialog = keyboardEvent.currentTarget as? HTMLDialogElement ?: return@keyDown
        val target = keyboardEvent.target as? HTMLButtonElement ?: return@keyDown
        when (keyboardEvent.key) {
            "ArrowLeft", "ArrowRight" -> {
                keyboardEvent.preventDefault()
                if (target.getAttribute("data-action") == "cancel") {
                    dialog.confirmButton().focus()
                } else {
                    dialog.cancelButton().focus()
                }
            }
        }
    }

    fun attach(dialog: HTMLDialogElement) {
        dialog.cancelButton().addEventListener("click", cancelClickListener)
        dialog.confirmButton().addEventListener("click", confirmClickListener)
        dialog.addEventListener("cancel", cancelListener)
        dialog.addEventListener("keydown", keyDownListener)
    }

    fun detach(dialog: HTMLDialogElement) {
        dialog.cancelButton().removeEventListener("click", cancelClickListener)
        dialog.confirmButton().removeEventListener("click", confirmClickListener)
        dialog.removeEventListener("cancel", cancelListener)
        dialog.removeEventListener("keydown", keyDownListener)
    }

    private fun dismiss(button: HTMLButtonElement?) {
        val current = callbacks()
        val dialog = button?.closest("dialog") as? HTMLDialogElement
        if (!current.isSaving && dialog != null) {
            dialog.close()
            restoreDeleteFocus()
            current.onDismiss()
        }
    }
}

private fun restoreDeleteFocus() {
    (document.querySelector("[data-testid='profile-editor-delete']") as? HTMLButtonElement)?.focus()
}

private fun createDialogButton(testId: String, label: String): HTMLButtonElement {
    return (document.createElement("button") as HTMLButtonElement).apply {
        type = "button"
        textContent = label
        setAttribute("data-testid", testId)
        setAttribute("data-action", if (label == "Cancel") "cancel" else "confirm")
        setAttribute("aria-label", label)
        if (label == "Cancel") {
            setAttribute("autofocus", "")
        }
    }
}

private fun HTMLDialogElement.cancelButton(): HTMLButtonElement {
    return requireNotNull(querySelector("[data-testid='profile-editor-delete-cancel']") as? HTMLButtonElement)
}

private fun HTMLDialogElement.confirmButton(): HTMLButtonElement {
    return requireNotNull(querySelector("[data-testid='profile-editor-delete-confirm']") as? HTMLButtonElement)
}

private fun HTMLDialogElement.titleElement(): HTMLElement {
    return requireNotNull(querySelector("#$PROFILE_EDITOR_DELETE_TITLE_ID") as? HTMLElement)
}

private fun HTMLDialogElement.descriptionElement(): HTMLElement {
    return requireNotNull(querySelector("#$PROFILE_EDITOR_DELETE_DESCRIPTION_ID") as? HTMLElement)
}

private fun dialogStyle(background: Color, foreground: Color): String {
    return "box-sizing:border-box;position:fixed;inset:0;width:min(560px,calc(100vw - 64px));" +
        "height:fit-content;margin:auto;" +
        "padding:${StreamCoreWebDimens.PanelPadding.value}px;border:1px solid rgba(255,255,255,.14);" +
        "border-radius:20px;background:${background.cssColor()};color:${foreground.cssColor()};" +
        "display:flex;flex-direction:column;gap:24px;box-shadow:0 24px 80px rgba(0,0,0,.55);"
}

private fun dialogButtonStyle(background: Color, foreground: Color): String {
    return "box-sizing:border-box;min-height:${StreamCoreWebDimens.ControlHeight.value}px;" +
        "padding:0 ${StreamCoreWebDimens.ActionPadding.value}px;border:2px solid transparent;" +
        "border-radius:${StreamCoreWebDimens.HtmlInputRadius.value}px;background:${background.cssColor()};" +
        "color:${foreground.cssColor()};font:600 ${StreamCoreWebDimens.HtmlInputFontSize.value}px system-ui,Segoe UI,Arial,sans-serif;cursor:pointer;"
}

private fun Color.cssColor(): String {
    val argb = toArgb()
    return "rgb(${(argb shr 16) and 0xff},${(argb shr 8) and 0xff},${argb and 0xff})"
}

private const val PROFILE_EDITOR_DELETE_DIALOG_ID = "streamcore-profile-editor-delete-dialog"
private const val PROFILE_EDITOR_DELETE_TITLE_ID = "streamcore-profile-editor-delete-title"
private const val PROFILE_EDITOR_DELETE_DESCRIPTION_ID = "streamcore-profile-editor-delete-description"
