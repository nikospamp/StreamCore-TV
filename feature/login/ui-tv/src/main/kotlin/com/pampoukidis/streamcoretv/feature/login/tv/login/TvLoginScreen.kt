package com.pampoukidis.streamcoretv.feature.login.tv.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginBackground
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginForm
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginFormLayout
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginFormModifiers
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginHeader
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.data.LoginBackgroundVariant

@Composable
fun TvLoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formModifiers = rememberTvLoginFormModifiers()

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
                    LoginHeader(
                        titleModifier = Modifier.padding(top = StreamCoreDimens.Spacing.Small),
                        subtitleModifier = Modifier.padding(
                            top = StreamCoreDimens.Spacing.Small,
                            bottom = StreamCoreDimens.Spacing.Small,
                        ),
                    )
                    LoginForm(
                        state = state,
                        onAction = onAction,
                        modifiers = formModifiers,
                        primaryButton = { text, onClick, enabled, loading, buttonModifier ->
                            StreamCoreTvButton(
                                text = text,
                                onClick = onClick,
                                enabled = enabled,
                                loading = loading,
                                shape = StreamCoreControlDefaults.style().buttonShape,
                                contentAlignment = Alignment.CenterHorizontally,
                                modifier = buttonModifier.defaultMinSize(
                                    minHeight = StreamCoreDimens.Button.MinHeight,
                                ),
                            )
                        },
                        secondaryButton = { text, onClick, enabled, buttonModifier ->
                            StreamCoreTvTextButton(
                                text = text,
                                onClick = onClick,
                                enabled = enabled,
                                modifier = buttonModifier,
                            )
                        },
                        passwordVisibilityControl = { onClick, enabled, controlModifier, content ->
                            StreamCoreTvIconButton(
                                onClick = onClick,
                                enabled = enabled,
                                modifier = controlModifier,
                                content = content,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun rememberTvLoginFormModifiers(): LoginFormModifiers {
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val passwordVisibilityFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val forgotPasswordFocusRequester = remember { FocusRequester() }
    val createAccountFocusRequester = remember { FocusRequester() }
    val helpFocusRequester = remember { FocusRequester() }
    var isPasswordFieldFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    return LoginFormModifiers(
        identifier = Modifier
            .focusRequester(emailFocusRequester)
            .focusProperties {
                down = passwordFocusRequester
            },
        password = Modifier
            .focusRequester(passwordFocusRequester)
            .onFocusChanged { isPasswordFieldFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                // Keep caret navigation while editing; otherwise Right reaches the reveal action.
                if (isPasswordFieldFocused && !isImeVisible && event.key == Key.DirectionRight) {
                    if (event.type == KeyEventType.KeyDown) {
                        passwordVisibilityFocusRequester.requestFocus()
                    }
                    true
                } else {
                    false
                }
            }
            .focusProperties {
                up = emailFocusRequester
                down = submitFocusRequester
            },
        passwordVisibility = Modifier
            .focusRequester(passwordVisibilityFocusRequester)
            .focusProperties {
                left = passwordFocusRequester
                previous = passwordFocusRequester
                next = submitFocusRequester
            },
        submit = Modifier
            .focusRequester(submitFocusRequester)
            .focusProperties {
                up = passwordFocusRequester
                down = forgotPasswordFocusRequester
            },
        forgotPassword = Modifier
            .focusRequester(forgotPasswordFocusRequester)
            .focusProperties {
                up = submitFocusRequester
                right = createAccountFocusRequester
                down = helpFocusRequester
            },
        createAccount = Modifier
            .focusRequester(createAccountFocusRequester)
            .focusProperties {
                up = submitFocusRequester
                left = forgotPasswordFocusRequester
                down = helpFocusRequester
            },
        help = Modifier
            .focusRequester(helpFocusRequester)
            .focusProperties {
                up = forgotPasswordFocusRequester
            },
    )
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
