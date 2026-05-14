package com.pampoukidis.streamcoretv.feature.login.common.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginUiState

@Composable
fun LoginMaterialForm(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.EmailChanged(it)) },
            label = { Text(text = stringResource(R.string.login_email_label)) },
            singleLine = true,
            isError = state.emailError != null,
            supportingText = state.emailError?.let { { Text(text = it.text()) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginTestTags.EmailField),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = { Text(text = stringResource(R.string.login_password_label)) },
            singleLine = true,
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { { Text(text = it.passwordText()) } },
            visualTransformation = PasswordVisualTransformation(),
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
                email = "lead@streamcore.tv",
                password = "password",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}