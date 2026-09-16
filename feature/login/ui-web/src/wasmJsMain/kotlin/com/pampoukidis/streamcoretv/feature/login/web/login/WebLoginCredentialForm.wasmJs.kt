package com.pampoukidis.streamcoretv.feature.login.web.login

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebControlStyle
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
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
    val controls = StreamCoreWebControlStyle(StreamCoreControlDefaults.style())
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
        transparent = MaterialTheme.colorScheme.transparentContainer,
        foreground = MaterialTheme.colorScheme.onSurface,
        supporting = MaterialTheme.colorScheme.onSurfaceVariant,
        outline = MaterialTheme.colorScheme.outline,
        primary = controls.roles.primary,
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
            form.styleElement().textContent = focusStyle(colors, controls)
            form.label(LOGIN_IDENTIFIER_LABEL_ID).textContent = identifierLabel
            form.label(LOGIN_PASSWORD_LABEL_ID).textContent = passwordLabel
            if (identifierInput.value != identifier) identifierInput.value = identifier
            if (passwordInput.value != password) passwordInput.value = password
            identifierInput.disabled = isLoading
            passwordInput.disabled = isLoading
            visibility.disabled = isLoading
            submit.setAttribute("aria-label", submitLabel)
            submit.disabled = isLoading
            submit.textContent = if (isLoading) "Signing in…" else submitLabel
            form.setAttribute("aria-busy", isLoading.toString())
            updateError(form, identifierInput, LOGIN_IDENTIFIER_ERROR_ID, identifierError, colors)
            updateError(form, passwordInput, LOGIN_PASSWORD_ERROR_ID, passwordError, colors)
            val passwordVisible = passwordInput.type == "text"
            updateVisibilityIcon(visibility, passwordVisible, if (passwordVisible) hidePasswordLabel else showPasswordLabel)
            visibility.setAttribute("aria-pressed", passwordVisible.toString())
        },
        onRelease = { form -> listeners.detach(form) },
        modifier = modifier.height(
            StreamCoreWebDimens.CredentialFormHeight + (if (identifierError != null) StreamCoreWebDimens.CredentialErrorHeight else 0.dp) +
                (if (passwordError != null) StreamCoreWebDimens.CredentialErrorHeight else 0.dp),
        ),
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
            updateVisibilityIcon(button, passwordVisible, if (passwordVisible) current.hidePasswordLabel else current.showPasswordLabel)
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
    val transparent: Color,
    val foreground: Color,
    val supporting: Color,
    val outline: Color,
    val primary: Color,
    val error: Color,
    val disabled: Color,
)

private fun formStyle(colors: LoginFormColors): String {
    return "box-sizing:border-box;width:100%;margin:0;display:flex;flex-direction:column;" +
        "gap:8px;padding-top:8px;color:${colors.foreground.cssColor()};font-family:system-ui,Segoe UI,Arial,sans-serif;"
}

// Focus paints inside each rounded control; an external outline is clipped by the HTML host.
private fun focusStyle(colors: LoginFormColors, controls: StreamCoreWebControlStyle): String {
    return """
        .streamcore-login-field{--field-focus:${colors.primary.cssColor()};position:relative;display:flex;flex-direction:column;gap:6px;min-width:0}
        .streamcore-login-field label{position:absolute;top:-8px;left:12px;z-index:1;padding:0 4px;background:${colors.surface.cssColor()};font-size:12px;font-weight:400;line-height:16px;color:${colors.supporting.cssColor()}}
        .streamcore-login-input-row{position:relative;display:flex;align-items:stretch;min-width:0}
        .streamcore-login-input-row input{box-sizing:border-box;min-width:0;width:100%;height:${StreamCoreWebDimens.ControlHeight.value}px;padding:0 ${StreamCoreWebDimens.HtmlInputPadding.value}px;border:1px solid ${colors.outline.cssColor()};${controls.input()}background:${colors.surface.cssColor()};color:${colors.foreground.cssColor()};outline:none}
        .streamcore-login-input-row:has(button) input{padding-right:56px}
        .streamcore-login-input-row button{position:absolute;right:6px;top:8px;width:40px;height:40px;min-width:40px;display:grid;place-items:center}
        .streamcore-login-input-row button svg{width:24px;height:24px;fill:currentColor}
        [data-testid='${LoginTestTags.SubmitButton}'],[data-testid='${LoginTestTags.PasswordVisibilityToggle}']{box-sizing:border-box;min-height:${StreamCoreWebDimens.ControlHeight.value}px;padding:0 ${StreamCoreWebDimens.ActionPadding.value}px;border:${StreamCoreWebDimens.FocusOuterBorder.value}px solid transparent;${controls.button()}}
        [data-testid='${LoginTestTags.SubmitButton}']{width:100%;margin-top:24px;${controls.button()}}
        [data-testid='${LoginTestTags.PasswordVisibilityToggle}']{min-height:40px;padding:0;${controls.button(colors.transparent, colors.foreground)}}
        .streamcore-login-input-row input:focus{outline:none;border-color:var(--field-focus);box-shadow:inset 0 0 0 1px var(--field-focus)}
        .streamcore-login-field:has(input:focus) label{color:var(--field-focus)}
        .streamcore-login-field:has(input[aria-invalid='true']){--field-focus:${colors.error.cssColor()}}
        .streamcore-login-input-row input[aria-invalid='true']{border-color:${colors.error.cssColor()}}
        .streamcore-login-error{min-height:20px;font-size:14px;font-weight:500;line-height:20px;overflow-wrap:anywhere}
        .streamcore-login-input-row input:disabled{cursor:default;opacity:${controls.roles.disabledInputOpacity}}
        ${controls.buttonStates("[data-testid='${LoginTestTags.SubmitButton}']")}
        ${controls.buttonStates("[data-testid='${LoginTestTags.PasswordVisibilityToggle}']")}
        [data-testid='${LoginTestTags.SubmitButton}']:focus-visible,[data-testid='${LoginTestTags.PasswordVisibilityToggle}']:focus-visible{outline:none;border-color:${colors.primary.cssColor()}}
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

// Exact vector geometry from core/ui's ic_visibility_24 / ic_visibility_off_24 assets.
private fun updateVisibilityIcon(button: HTMLButtonElement, visible: Boolean, label: String) {
    button.setAttribute("aria-label", label)
    if (button.getAttribute("data-visible") == visible.toString()) return
    button.setAttribute("data-visible", visible.toString())
    button.textContent = ""
    val svg = document.createElementNS("http://www.w3.org/2000/svg", "svg")
    svg.setAttribute("viewBox", "0 0 24 24")
    svg.setAttribute("aria-hidden", "true")
    val path = document.createElementNS("http://www.w3.org/2000/svg", "path")
    path.setAttribute("d", if (visible) "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z" else "M2,4.27L4.28,2 22,19.72 19.73,22l-3.05,-3.05C15.22,19.5 13.65,19.8 12,19.8 7,19.8 2.73,16.69 1,12.3c0.8,-2.03 2.16,-3.76 3.87,-5.03L2,4.27zM7.53,9.8C7.19,10.53 7,11.39 7,12.3c0,2.76 2.24,5 5,5 0.91,0 1.77,-0.19 2.5,-0.53L12.72,15c-0.24,0.06 -0.48,0.1 -0.72,0.1 -1.66,0 -3,-1.34 -3,-3 0,-0.24 0.04,-0.48 0.1,-0.72L7.53,9.8zM12,4.8c5,0 9.27,3.11 11,7.5 -0.5,1.27 -1.27,2.43 -2.23,3.42L17.6,12.55c0,-0.08 0,-0.17 0,-0.25 0,-2.76 -2.24,-5 -5,-5 -0.08,0 -0.17,0 -0.25,0L9.96,4.91C10.62,4.84 11.3,4.8 12,4.8zM12.45,9.32L15.58,12.45C15.51,10.8 14.2,9.49 12.45,9.32z")
    svg.appendChild(path)
    button.appendChild(svg)
}
