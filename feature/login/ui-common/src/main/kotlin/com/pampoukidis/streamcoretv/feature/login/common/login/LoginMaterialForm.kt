package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags

@Composable
fun LoginMaterialForm(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.identifier,
            onValueChange = { onAction(LoginAction.IdentifierChanged(it)) },
            label = { Text(text = stringResource(R.string.login_identifier_label)) },
            singleLine = true,
            isError = state.identifierError != null,
            supportingText = state.identifierError?.let { { Text(text = it.text()) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentType = ContentType.Username + ContentType.EmailAddress
                }
                .testTag(LoginTestTags.IdentifierField),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = { Text(text = stringResource(R.string.login_password_label)) },
            singleLine = true,
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { { Text(text = it.passwordText()) } },
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                val contentDescription = if (isPasswordVisible) {
                    stringResource(R.string.login_password_hide)
                } else {
                    stringResource(R.string.login_password_show)
                }
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier.testTag(LoginTestTags.PasswordVisibilityToggle),
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isPasswordVisible) {
                                R.drawable.ic_visibility_24
                            } else {
                                R.drawable.ic_visibility_off_24
                            },
                        ),
                        contentDescription = contentDescription,
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentType = ContentType.Password
                }
                .testTag(LoginTestTags.PasswordField),
        )
        StreamCoreButton(
            text = stringResource(R.string.login_continue),
            onClick = { onAction(LoginAction.Submit) },
            enabled = state.isSubmitEnabled && !state.isLoading,
            loading = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginTestTags.SubmitButton),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        ) {
            StreamCoreTextButton(
                text = stringResource(R.string.login_forgot_password),
                onClick = { onAction(LoginAction.ForgotPassword) },
                enabled = !state.isLoading,
                modifier = Modifier.testTag(LoginTestTags.ForgotPasswordButton),
            )
            StreamCoreTextButton(
                text = stringResource(R.string.login_create_account),
                onClick = { onAction(LoginAction.CreateAccount) },
                enabled = !state.isLoading,
                modifier = Modifier.testTag(LoginTestTags.CreateAccountButton),
            )
        }
        StreamCoreTextButton(
            text = stringResource(R.string.login_help),
            onClick = { onAction(LoginAction.Help) },
            enabled = !state.isLoading,
            modifier = Modifier.testTag(LoginTestTags.HelpButton),
        )
    }
}

@PreviewMobile
@Composable
private fun LoginMaterialFormPreview() {
    StreamCoreTVTheme {
        LoginMaterialForm(
            state = LoginUiState(
                identifier = "lead@streamcore.tv",
                password = "password",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}