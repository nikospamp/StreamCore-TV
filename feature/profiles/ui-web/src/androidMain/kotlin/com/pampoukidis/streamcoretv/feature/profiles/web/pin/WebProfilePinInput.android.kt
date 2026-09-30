package com.pampoukidis.streamcoretv.feature.profiles.web.pin

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinFailure
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinTestTags
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinUiState

@Composable
internal actual fun WebProfilePinInput(
    state: ProfilePinUiState,
    label: String,
    focusRequest: Int,
    onDraftChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onTab: (Boolean) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = state.draft,
        onValueChange = onDraftChanged,
        label = { Text(label) },
        singleLine = true,
        enabled = state.inputEnabled,
        isError = state.failure == ProfilePinFailure.Incorrect,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.testTag(ProfilePinTestTags.Input),
    )
}
