package com.pampoukidis.streamcoretv.feature.login.web.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginBackground
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.common.login.passwordText
import com.pampoukidis.streamcoretv.feature.login.common.login.text
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.data.LoginBackgroundVariant
import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError
import org.jetbrains.compose.resources.stringResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.login_continue
import streamcoretv.core.ui.generated.resources.login_create_account
import streamcoretv.core.ui.generated.resources.login_forgot_password
import streamcoretv.core.ui.generated.resources.login_help
import streamcoretv.core.ui.generated.resources.login_identifier_label
import streamcoretv.core.ui.generated.resources.login_password_hide
import streamcoretv.core.ui.generated.resources.login_password_label
import streamcoretv.core.ui.generated.resources.login_password_show
import streamcoretv.core.ui.generated.resources.login_subtitle
import streamcoretv.core.ui.generated.resources.login_title

@Composable
fun WebLoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
    auxiliaryActionsEnabled: Boolean = false,
    backendErrorMessage: String? = null,
) {
    val identifierFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val forgotFocus = remember { FocusRequester() }
    val createFocus = remember { FocusRequester() }
    val helpFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        identifierFocus.requestFocus()
    }

    LoginBackground(
        variant = LoginBackgroundVariant.Landscape,
        modifier = modifier.testTag(LoginTestTags.Root),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = StreamCoreWebDimens.ScreenHorizontal,
                    vertical = StreamCoreWebDimens.ScreenVertical,
                ),
        ) {
            StreamCoreWebPanel(
                modifier = Modifier
                    .width(StreamCoreWebDimens.PanelWidth)
                    .fillMaxHeight(),
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(Res.string.login_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = stringResource(Res.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = StreamCoreDimens.Spacing.Small,
                            bottom = StreamCoreDimens.Spacing.ExtraLarge,
                        ),
                    )
                    WebLoginForm(
                        state = state,
                        onAction = onAction,
                        identifierFocus = identifierFocus,
                        passwordFocus = passwordFocus,
                        submitFocus = submitFocus,
                        forgotFocus = forgotFocus,
                        createFocus = createFocus,
                        helpFocus = helpFocus,
                        auxiliaryActionsEnabled = auxiliaryActionsEnabled,
                        backendErrorMessage = backendErrorMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun WebLoginForm(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    identifierFocus: FocusRequester,
    passwordFocus: FocusRequester,
    submitFocus: FocusRequester,
    forgotFocus: FocusRequester,
    createFocus: FocusRequester,
    helpFocus: FocusRequester,
    auxiliaryActionsEnabled: Boolean,
    backendErrorMessage: String?,
) {
    val identifierError = state.identifierError?.text()
    val passwordError = state.passwordError?.passwordText()
    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
        WebLoginCredentialForm(
            identifier = state.identifier,
            password = state.password,
            identifierError = identifierError,
            passwordError = passwordError,
            isSubmitEnabled = state.isSubmitEnabled,
            isLoading = state.isLoading,
            onIdentifierChanged = { onAction(LoginAction.IdentifierChanged(it)) },
            onPasswordChanged = { onAction(LoginAction.PasswordChanged(it)) },
            onSubmit = { onAction(LoginAction.Submit) },
            identifierLabel = stringResource(Res.string.login_identifier_label),
            passwordLabel = stringResource(Res.string.login_password_label),
            showPasswordLabel = stringResource(Res.string.login_password_show),
            hidePasswordLabel = stringResource(Res.string.login_password_hide),
            submitLabel = stringResource(Res.string.login_continue),
            identifierFocus = identifierFocus,
            passwordFocus = passwordFocus,
            submitFocus = submitFocus,
            modifier = Modifier.fillMaxWidth(),
        )
        backendErrorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { error(message) },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StreamCoreWebButton(
                text = stringResource(Res.string.login_forgot_password),
                onClick = { onAction(LoginAction.ForgotPassword) },
                enabled = auxiliaryActionsEnabled && !state.isLoading,
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(forgotFocus)
                    .focusProperties {
                        up = submitFocus
                        right = createFocus
                        down = helpFocus
                    }
                    .testTag(LoginTestTags.ForgotPasswordButton),
            )
            StreamCoreWebButton(
                text = stringResource(Res.string.login_create_account),
                onClick = { onAction(LoginAction.CreateAccount) },
                enabled = auxiliaryActionsEnabled && !state.isLoading,
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(createFocus)
                    .focusProperties {
                        up = submitFocus
                        left = forgotFocus
                        down = helpFocus
                    }
                    .testTag(LoginTestTags.CreateAccountButton),
            )
        }
        StreamCoreWebButton(
            text = stringResource(Res.string.login_help),
            onClick = { onAction(LoginAction.Help) },
            enabled = auxiliaryActionsEnabled && !state.isLoading,
            variant = StreamCoreWebButtonVariant.Tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(helpFocus)
                .focusProperties { up = forgotFocus }
                .testTag(LoginTestTags.HelpButton),
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebLoginScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebLoginScreen(
            state = LoginUiState(),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebLoginLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebLoginScreen(
            state = LoginUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebLoginValidationErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebLoginScreen(
            state = LoginUiState(
                identifierError = LoginFieldError.Required,
                passwordError = LoginFieldError.Required,
            ),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebLoginBackendErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebLoginScreen(
            state = LoginUiState(
                identifier = "subscriber@example.test",
                password = "preview-only",
                isSubmitEnabled = true,
            ),
            onAction = {},
            backendErrorMessage = "The account service rejected these credentials. Check them and try again.",
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebLoginLongTextPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebLoginScreen(
            state = LoginUiState(
                identifier = "a-very-long-subscriber-identifier-used-to-verify-field-overflow@example.test",
                password = "preview-only",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}
