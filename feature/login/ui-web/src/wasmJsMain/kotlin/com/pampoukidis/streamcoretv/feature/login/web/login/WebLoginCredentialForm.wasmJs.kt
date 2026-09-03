package com.pampoukidis.streamcoretv.feature.login.web.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebLoginCredentialForm(
    identifier: String,
    password: String,
    identifierError: String?,
    passwordError: String?,
    isSubmitEnabled: Boolean,
    isLoading: Boolean,
    onIdentifierChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    identifierLabel: String,
    passwordLabel: String,
    showPasswordLabel: String,
    hidePasswordLabel: String,
    submitLabel: String,
    identifierFocus: FocusRequester,
    passwordFocus: FocusRequester,
    submitFocus: FocusRequester,
    modifier: Modifier,
) {
    val currentCallbacks = rememberUpdatedState(
        LoginCredentialCallbacks(
            isLoading = isLoading,
            showPasswordLabel = showPasswordLabel,
            hidePasswordLabel = hidePasswordLabel,
            onIdentifierChanged = onIdentifierChanged,
            onPasswordChanged = onPasswordChanged,
            onSubmit = onSubmit,
        ),
    )
    val listeners = remember {
        LoginCredentialListeners(callbacks = { currentCallbacks.value })
    }
    val colors = LoginFormColors(
        surface = MaterialTheme.colorScheme.surface,
        foreground = MaterialTheme.colorScheme.onSurface,
        supporting = MaterialTheme.colorScheme.onSurfaceVariant,
        outline = MaterialTheme.colorScheme.outline,
        primary = MaterialTheme.colorScheme.primary,
        onPrimary = MaterialTheme.colorScheme.onPrimary,
        error = MaterialTheme.colorScheme.error,
        disabled = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    HtmlElementView(
        factory = {
            createLoginForm(
                identifierLabel = identifierLabel,
                passwordLabel = passwordLabel,
                showPasswordLabel = showPasswordLabel,
                submitLabel = submitLabel,
                listeners = listeners,
            )
        },
        update = { form ->
            val identifierInput = form.input(LoginTestTags.IdentifierField)
            val passwordInput = form.input(LoginTestTags.PasswordField)
            val visibility = form.button(LoginTestTags.PasswordVisibilityToggle)
            val submit = form.button(LoginTestTags.SubmitButton)

            form.style.cssText = formStyle(colors)
            form.styleElement().textContent = focusStyle(colors)
            form.label(LOGIN_IDENTIFIER_LABEL_ID).textContent = identifierLabel
            form.label(LOGIN_PASSWORD_LABEL_ID).textContent = passwordLabel
            if (identifierInput.value != identifier) identifierInput.value = identifier
            if (passwordInput.value != password) passwordInput.value = password
            identifierInput.disabled = isLoading
            passwordInput.disabled = isLoading
            visibility.disabled = isLoading
            submit.disabled = isLoading
            submit.textContent = submitLabel
            form.setAttribute("aria-busy", isLoading.toString())
            updateError(form, identifierInput, LOGIN_IDENTIFIER_ERROR_ID, identifierError, colors)
            updateError(form, passwordInput, LOGIN_PASSWORD_ERROR_ID, passwordError, colors)
            val passwordVisible = passwordInput.type == "text"
            visibility.textContent = if (passwordVisible) hidePasswordLabel else showPasswordLabel
            visibility.setAttribute("aria-label", visibility.textContent.orEmpty())
            visibility.setAttribute("aria-pressed", passwordVisible.toString())
        },
        onRelease = { form -> listeners.detach(form) },
        modifier = modifier.height(StreamCoreWebDimens.CredentialFormHeight),
    )

    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        (document.querySelector("[data-testid='${LoginTestTags.IdentifierField}']") as? HTMLInputElement)
            ?.focus()
    }
}

private data class LoginCredentialCallbacks(
    val isLoading: Boolean,
    val showPasswordLabel: String,
    val hidePasswordLabel: String,
    val onIdentifierChanged: (String) -> Unit,
    val onPasswordChanged: (String) -> Unit,
    val onSubmit: () -> Unit,
)

private class LoginCredentialListeners(
    private val callbacks: () -> LoginCredentialCallbacks,
) {
    private val identifierInputListener: (Event) -> Unit = { event ->
        val input = event.currentTarget as? HTMLInputElement
        if (input != null) callbacks().onIdentifierChanged(input.value)
    }
    private val passwordInputListener: (Event) -> Unit = { event ->
        val input = event.currentTarget as? HTMLInputElement
        if (input != null) callbacks().onPasswordChanged(input.value)
    }
    private val visibilityClickListener: (Event) -> Unit = { event ->
        event.preventDefault()
        val button = event.currentTarget as? HTMLButtonElement
        val form = button?.form
        val password = form?.input(LoginTestTags.PasswordField)
        if (password != null && !callbacks().isLoading) {
            val current = callbacks()
            val selectionStart = password.selectionStart
            val selectionEnd = password.selectionEnd
            password.type = if (password.type == "password") "text" else "password"
            val passwordVisible = password.type == "text"
            button.textContent = if (passwordVisible) current.hidePasswordLabel else current.showPasswordLabel
            button.setAttribute("aria-label", button.textContent.orEmpty())
            button.setAttribute("aria-pressed", passwordVisible.toString())
            password.focus()
            if (selectionStart != null && selectionEnd != null) {
                password.setSelectionRange(selectionStart, selectionEnd)
            }
        }
    }
    private val submitListener: (Event) -> Unit = submit@{ event ->
        event.preventDefault()
        val form = event.currentTarget as? HTMLFormElement ?: return@submit
        val current = callbacks()
        if (current.isLoading) return@submit
        current.onIdentifierChanged(form.input(LoginTestTags.IdentifierField).value)
        current.onPasswordChanged(form.input(LoginTestTags.PasswordField).value)
        current.onSubmit()
    }

    fun attach(form: HTMLFormElement) {
        form.input(LoginTestTags.IdentifierField).addEventListener("input", identifierInputListener)
        form.input(LoginTestTags.PasswordField).addEventListener("input", passwordInputListener)
        form.button(LoginTestTags.PasswordVisibilityToggle).addEventListener("click", visibilityClickListener)
        form.addEventListener("submit", submitListener)
    }

    fun detach(form: HTMLFormElement) {
        form.input(LoginTestTags.IdentifierField).removeEventListener("input", identifierInputListener)
        form.input(LoginTestTags.PasswordField).removeEventListener("input", passwordInputListener)
        form.button(LoginTestTags.PasswordVisibilityToggle).removeEventListener("click", visibilityClickListener)
        form.removeEventListener("submit", submitListener)
    }
}

private fun createLoginForm(
    identifierLabel: String,
    passwordLabel: String,
    showPasswordLabel: String,
    submitLabel: String,
    listeners: LoginCredentialListeners,
): HTMLFormElement {
    return (document.createElement("form") as HTMLFormElement).apply {
        setAttribute("data-testid", LOGIN_FORM_TEST_ID)
        setAttribute("aria-label", "Sign in credentials")
        noValidate = true
        appendChild((document.createElement("style") as HTMLElement).apply { setAttribute("data-login-style", "true") })
        appendChild(createField(
            labelId = LOGIN_IDENTIFIER_LABEL_ID,
            label = identifierLabel,
            inputId = LOGIN_IDENTIFIER_INPUT_ID,
            testId = LoginTestTags.IdentifierField,
            type = "email",
            autocomplete = "username",
            errorId = LOGIN_IDENTIFIER_ERROR_ID,
        ))
        appendChild(createField(
            labelId = LOGIN_PASSWORD_LABEL_ID,
            label = passwordLabel,
            inputId = LOGIN_PASSWORD_INPUT_ID,
            testId = LoginTestTags.PasswordField,
            type = "password",
            autocomplete = "current-password",
            errorId = LOGIN_PASSWORD_ERROR_ID,
            trailingButton = createButton(
                testId = LoginTestTags.PasswordVisibilityToggle,
                label = showPasswordLabel,
                type = "button",
            ),
        ))
        appendChild(createButton(LoginTestTags.SubmitButton, submitLabel, "submit"))
        listeners.attach(this)
    }
}

private fun createField(
    labelId: String,
    label: String,
    inputId: String,
    testId: String,
    type: String,
    autocomplete: String,
    errorId: String,
    trailingButton: HTMLButtonElement? = null,
): HTMLElement {
    return (document.createElement("div") as HTMLElement).apply {
        className = "streamcore-login-field"
        appendChild((document.createElement("label") as HTMLElement).apply {
            id = labelId
            textContent = label
            setAttribute("for", inputId)
        })
        appendChild((document.createElement("div") as HTMLElement).apply {
            className = "streamcore-login-input-row"
            appendChild((document.createElement("input") as HTMLInputElement).apply {
                id = inputId
                this.type = type
                setAttribute("data-testid", testId)
                setAttribute("autocomplete", autocomplete)
                setAttribute("maxlength", if (type == "password") "128" else "254")
                setAttribute("aria-labelledby", labelId)
                setAttribute("autocapitalize", "none")
                spellcheck = false
            })
            if (trailingButton != null) appendChild(trailingButton)
        })
        appendChild((document.createElement("div") as HTMLElement).apply {
            id = errorId
            className = "streamcore-login-error"
            setAttribute("role", "alert")
            setAttribute("aria-live", "polite")
            hidden = true
        })
    }
}

private fun createButton(testId: String, label: String, type: String): HTMLButtonElement {
    return (document.createElement("button") as HTMLButtonElement).apply {
        this.type = type
        textContent = label
        setAttribute("data-testid", testId)
        setAttribute("aria-label", label)
    }
}

private fun updateError(
    form: HTMLFormElement,
    input: HTMLInputElement,
    errorId: String,
    message: String?,
    colors: LoginFormColors,
) {
    val error = requireNotNull(form.querySelector("#$errorId") as? HTMLElement)
    if (message == null) {
        input.removeAttribute("aria-invalid")
        input.removeAttribute("aria-describedby")
        input.removeAttribute("aria-errormessage")
        error.textContent = ""
        error.hidden = true
    } else {
        input.setAttribute("aria-invalid", "true")
        input.setAttribute("aria-describedby", errorId)
        input.setAttribute("aria-errormessage", errorId)
        error.style.color = colors.error.cssColor()
        error.textContent = message
        error.hidden = false
    }
}

private fun HTMLFormElement.input(testId: String): HTMLInputElement {
    return requireNotNull(querySelector("[data-testid='$testId']") as? HTMLInputElement)
}

private fun HTMLFormElement.button(testId: String): HTMLButtonElement {
    return requireNotNull(querySelector("[data-testid='$testId']") as? HTMLButtonElement)
}

private fun HTMLFormElement.label(id: String): HTMLElement {
    return requireNotNull(querySelector("#$id") as? HTMLElement)
}

private fun HTMLFormElement.styleElement(): HTMLElement {
    return requireNotNull(querySelector("[data-login-style='true']") as? HTMLElement)
}

private data class LoginFormColors(
    val surface: Color,
    val foreground: Color,
    val supporting: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val error: Color,
    val disabled: Color,
)

private fun formStyle(colors: LoginFormColors): String {
    return "box-sizing:border-box;width:100%;margin:0;display:flex;flex-direction:column;" +
        "gap:12px;color:${colors.foreground.cssColor()};font-family:system-ui,Segoe UI,Arial,sans-serif;"
}

private fun focusStyle(colors: LoginFormColors): String {
    return """
        .streamcore-login-field{display:flex;flex-direction:column;gap:6px;min-width:0}
        .streamcore-login-field label{font-size:14px;font-weight:600;line-height:20px;color:${colors.supporting.cssColor()}}
        .streamcore-login-input-row{display:flex;align-items:stretch;gap:8px;min-width:0}
        .streamcore-login-input-row input{box-sizing:border-box;min-width:0;width:100%;height:${StreamCoreWebDimens.ControlHeight.value}px;padding:0 ${StreamCoreWebDimens.HtmlInputPadding.value}px;border:${StreamCoreWebDimens.FocusOuterBorder.value}px solid ${colors.outline.cssColor()};border-radius:${StreamCoreWebDimens.HtmlInputRadius.value}px;background:${colors.surface.cssColor()};color:${colors.foreground.cssColor()};font:400 ${StreamCoreWebDimens.HtmlInputFontSize.value}px system-ui,Segoe UI,Arial,sans-serif;outline:none}
        .streamcore-login-input-row button{flex:0 0 auto;min-width:112px}
        [data-testid='${LoginTestTags.SubmitButton}'],[data-testid='${LoginTestTags.PasswordVisibilityToggle}']{box-sizing:border-box;min-height:${StreamCoreWebDimens.ControlHeight.value}px;padding:0 ${StreamCoreWebDimens.ActionPadding.value}px;border:${StreamCoreWebDimens.FocusOuterBorder.value}px solid transparent;border-radius:${StreamCoreWebDimens.HtmlInputRadius.value}px;font:600 16px system-ui,Segoe UI,Arial,sans-serif;cursor:pointer}
        [data-testid='${LoginTestTags.SubmitButton}']{width:100%;background:${colors.primary.cssColor()};color:${colors.onPrimary.cssColor()}}
        [data-testid='${LoginTestTags.PasswordVisibilityToggle}']{background:${colors.disabled.cssColor()};color:${colors.foreground.cssColor()}}
        .streamcore-login-input-row input:focus-visible,[data-testid='${LoginTestTags.SubmitButton}']:focus-visible,[data-testid='${LoginTestTags.PasswordVisibilityToggle}']:focus-visible{outline:${StreamCoreWebDimens.FocusBorder.value}px solid ${colors.primary.cssColor()};outline-offset:2px}
        .streamcore-login-input-row input[aria-invalid='true']{border-color:${colors.error.cssColor()}}
        .streamcore-login-error{min-height:20px;font-size:14px;font-weight:500;line-height:20px;overflow-wrap:anywhere}
        button:disabled,input:disabled{cursor:default;opacity:.55}
        @media (prefers-reduced-motion:reduce){*{scroll-behavior:auto!important}}
    """.trimIndent()
}

private fun Color.cssColor(): String {
    val argb = toArgb()
    return "rgb(${(argb shr 16) and 0xff},${(argb shr 8) and 0xff},${argb and 0xff})"
}

private const val LOGIN_FORM_TEST_ID = "login:credentials-form"
private const val LOGIN_IDENTIFIER_INPUT_ID = "streamcore-login-identifier"
private const val LOGIN_PASSWORD_INPUT_ID = "streamcore-login-password"
private const val LOGIN_IDENTIFIER_LABEL_ID = "streamcore-login-identifier-label"
private const val LOGIN_PASSWORD_LABEL_ID = "streamcore-login-password-label"
private const val LOGIN_IDENTIFIER_ERROR_ID = "streamcore-login-identifier-error"
private const val LOGIN_PASSWORD_ERROR_ID = "streamcore-login-password-error"
