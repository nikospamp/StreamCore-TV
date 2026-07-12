package com.pampoukidis.streamcoretv.feature.login.tv.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginBackground
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.common.login.passwordText
import com.pampoukidis.streamcoretv.feature.login.common.login.text
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.data.LoginBackgroundVariant

@Composable
fun TvLoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val forgotPasswordFocusRequester = remember { FocusRequester() }
    val createAccountFocusRequester = remember { FocusRequester() }
    val helpFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    LoginBackground(
        variant = LoginBackgroundVariant.Landscape,
        modifier = modifier.testTag(LoginTestTags.Root),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                    vertical = StreamCoreDimens.Tv.Screen.VerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = StreamCoreDimens.Elevation.Medium,
                modifier = Modifier
                    .width(StreamCoreDimens.Tv.Panel.Width)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.padding(StreamCoreDimens.Tv.Panel.Padding),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = StreamCoreDimens.Spacing.Small),
                    )
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = StreamCoreDimens.Spacing.Small,
                            bottom = StreamCoreDimens.Spacing.Small,
                        ),
                    )
                    TvLoginForm(
                        state = state,
                        onAction = onAction,
                        emailFocusRequester = emailFocusRequester,
                        passwordFocusRequester = passwordFocusRequester,
                        submitFocusRequester = submitFocusRequester,
                        forgotPasswordFocusRequester = forgotPasswordFocusRequester,
                        createAccountFocusRequester = createAccountFocusRequester,
                        helpFocusRequester = helpFocusRequester,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvLoginForm(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    submitFocusRequester: FocusRequester,
    forgotPasswordFocusRequester: FocusRequester,
    createAccountFocusRequester: FocusRequester,
    helpFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
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
                .focusRequester(emailFocusRequester)
                .focusProperties {
                    down = passwordFocusRequester
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
                                R.drawable.ic_visibility_off_24
                            } else {
                                R.drawable.ic_visibility_24
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
                .focusRequester(passwordFocusRequester)
                .focusProperties {
                    up = emailFocusRequester
                    down = submitFocusRequester
                }
                .testTag(LoginTestTags.PasswordField),
        )
        StreamCoreTvButton(
            text = stringResource(R.string.login_continue),
            enabled = state.isSubmitEnabled && !state.isLoading,
            loading = state.isLoading,
            onClick = { onAction(LoginAction.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(submitFocusRequester)
                .focusProperties {
                    up = passwordFocusRequester
                    down = forgotPasswordFocusRequester
                }
                .testTag(LoginTestTags.SubmitButton),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        ) {
            StreamCoreTvButton(
                text = stringResource(R.string.login_forgot_password),
                enabled = !state.isLoading,
                onClick = { onAction(LoginAction.ForgotPassword) },
                modifier = Modifier
                    .focusRequester(forgotPasswordFocusRequester)
                    .focusProperties {
                        up = submitFocusRequester
                        right = createAccountFocusRequester
                        down = helpFocusRequester
                    }
                    .testTag(LoginTestTags.ForgotPasswordButton),
            )
            StreamCoreTvButton(
                text = stringResource(R.string.login_create_account),
                enabled = !state.isLoading,
                onClick = { onAction(LoginAction.CreateAccount) },
                modifier = Modifier
                    .focusRequester(createAccountFocusRequester)
                    .focusProperties {
                        up = submitFocusRequester
                        left = forgotPasswordFocusRequester
                        down = helpFocusRequester
                    }
                    .testTag(LoginTestTags.CreateAccountButton),
            )
        }
        StreamCoreTvButton(
            text = stringResource(R.string.login_help),
            enabled = !state.isLoading,
            onClick = { onAction(LoginAction.Help) },
            modifier = Modifier
                .focusRequester(helpFocusRequester)
                .focusProperties {
                    up = forgotPasswordFocusRequester
                }
                .testTag(LoginTestTags.HelpButton),
        )
    }
}

@PreviewTV
@Composable
private fun TvLoginScreenPreview() {
    StreamCoreTheme {
        TvLoginScreen(
            state = LoginUiState(
                identifier = "lead@streamcore.tv",
                password = "password",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}
