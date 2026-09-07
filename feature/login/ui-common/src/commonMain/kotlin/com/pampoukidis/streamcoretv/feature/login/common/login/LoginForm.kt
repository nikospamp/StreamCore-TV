package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags

@Composable
fun LoginForm(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
    layout: LoginFormLayout = LoginFormLayout(),
    modifiers: LoginFormModifiers = LoginFormModifiers(),
    primaryButton: @Composable (
        text: String,
        onClick: () -> Unit,
        enabled: Boolean,
        loading: Boolean,
        modifier: Modifier,
    ) -> Unit = { text, onClick, enabled, loading, buttonModifier ->
        StreamCoreButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            loading = loading,
            modifier = buttonModifier,
        )
    },
    secondaryButton: @Composable (
        text: String,
        onClick: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
    ) -> Unit = { text, onClick, enabled, buttonModifier ->
        StreamCoreTextButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
        )
    },
    passwordVisibilityControl: @Composable (
        onClick: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) -> Unit = { onClick, _, controlModifier, content ->
        // Preserve the touch form's existing reveal action while consolidating platform rendering.
        IconButton(
            onClick = onClick,
            modifier = controlModifier,
            content = content,
        )
    },
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(layout.verticalSpacing),
    ) {
        OutlinedTextField(
            value = state.identifier,
            onValueChange = { onAction(LoginAction.IdentifierChanged(it)) },
            label = { Text(text = stringResource(Res.string.login_identifier_label)) },
            singleLine = true,
            isError = state.identifierError != null,
            supportingText = state.identifierError?.let { { Text(text = it.text()) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
            modifier = modifiers.identifier
                .fillMaxWidth()
                .semantics {
                    contentType = ContentType.Username + ContentType.EmailAddress
                }
                .testTag(LoginTestTags.IdentifierField),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = { Text(text = stringResource(Res.string.login_password_label)) },
            singleLine = true,
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { { Text(text = it.passwordText()) } },
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                passwordVisibilityControl(
                    { isPasswordVisible = !isPasswordVisible },
                    !state.isLoading,
                    modifiers.passwordVisibility.testTag(LoginTestTags.PasswordVisibilityToggle),
                ) {
                    LoginPasswordVisibilityIcon(isPasswordVisible = isPasswordVisible)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAction(LoginAction.Submit) },
            ),
            enabled = !state.isLoading,
            modifier = modifiers.password
                .fillMaxWidth()
                .semantics {
                    contentType = ContentType.Password
                }
                .testTag(LoginTestTags.PasswordField),
        )
        primaryButton(
            stringResource(Res.string.login_continue),
            { onAction(LoginAction.Submit) },
            state.isSubmitEnabled && !state.isLoading,
            state.isLoading,
            modifiers.submit
                .fillMaxWidth()
                .padding(top = layout.submitTopPadding)
                .testTag(LoginTestTags.SubmitButton),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(layout.secondaryActionsSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = layout.secondaryActionsTopPadding),
        ) {
            secondaryButton(
                stringResource(Res.string.login_forgot_password),
                { onAction(LoginAction.ForgotPassword) },
                !state.isLoading,
                modifiers.forgotPassword.testTag(LoginTestTags.ForgotPasswordButton),
            )
            secondaryButton(
                stringResource(Res.string.login_create_account),
                { onAction(LoginAction.CreateAccount) },
                !state.isLoading,
                modifiers.createAccount.testTag(LoginTestTags.CreateAccountButton),
            )
        }
        secondaryButton(
            stringResource(Res.string.login_help),
            { onAction(LoginAction.Help) },
            !state.isLoading,
            modifiers.help.testTag(LoginTestTags.HelpButton),
        )
    }
}

@Preview
@Composable
private fun LoginFormPreview() {
    StreamCoreTheme {
        LoginForm(
            state = LoginUiState(
                identifier = "lead@streamcore.tv",
                password = "password",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}