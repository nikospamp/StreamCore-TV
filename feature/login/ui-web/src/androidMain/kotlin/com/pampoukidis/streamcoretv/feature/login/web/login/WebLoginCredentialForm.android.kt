package com.pampoukidis.streamcoretv.feature.login.web.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags

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
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = identifier,
            onValueChange = onIdentifierChanged,
            label = { Text(identifierLabel) },
            singleLine = true,
            enabled = !isLoading,
            isError = identifierError != null,
            supportingText = identifierError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(identifierFocus)
                .focusProperties { down = passwordFocus }
                .testTag(LoginTestTags.IdentifierField),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text(passwordLabel) },
            singleLine = true,
            enabled = !isLoading,
            isError = passwordError != null,
            supportingText = passwordError?.let { message -> { Text(message) } },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon = {
                StreamCoreWebButton(
                    text = if (isPasswordVisible) hidePasswordLabel else showPasswordLabel,
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    variant = StreamCoreWebButtonVariant.Tertiary,
                    modifier = Modifier.testTag(LoginTestTags.PasswordVisibilityToggle),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus)
                .focusProperties { up = identifierFocus; down = submitFocus }
                .testTag(LoginTestTags.PasswordField),
        )
        StreamCoreWebButton(
            text = submitLabel,
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            loading = isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(submitFocus)
                .focusProperties { up = passwordFocus }
                .testTag(LoginTestTags.SubmitButton),
        )
    }
}
