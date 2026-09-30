package com.pampoukidis.streamcoretv.feature.profiles.mobile.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.pampoukidis.streamcore.sdk.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.*
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import org.jetbrains.compose.resources.stringResource

@Composable
fun MobileProfilePinScreen(
    state: ProfilePinUiState,
    onDraftChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputFocus = remember { FocusRequester() }
    LaunchedEffect(state.profile.id, state.inputRevision) {
        if (state.inputEnabled) inputFocus.requestFocus()
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize().testTag(ProfilePinTestTags.Root)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding, vertical = StreamCoreDimens.Spacing.Large),
        ) {
            ProfilePinIdentity(state.profile, StreamCoreDimens.Mobile.Profiles.AvatarSize)
            Text(stringResource(Res.string.profile_pin_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.profile_pin_prompt, state.digitCount), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChanged,
                enabled = state.inputEnabled,
                singleLine = true,
                label = { Text(stringResource(Res.string.profile_pin_input_label)) },
                isError = state.failure == ProfilePinFailure.Incorrect,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(StreamCoreControlDefaults.style().inputRadius),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.widthIn(max = StreamCoreDimens.Tv.Panel.Width).fillMaxWidth()
                    .focusRequester(inputFocus).testTag(ProfilePinTestTags.Input),
            )
            ProfilePinStatus(state.isSubmitting, state.failure, Modifier.widthIn(max = StreamCoreDimens.Tv.Panel.Width))
            if (state.canRetry) {
                StreamCoreButton(
                    text = stringResource(Res.string.profile_pin_retry),
                    onClick = onRetry,
                    enabled = true,
                    modifier = Modifier.testTag(ProfilePinTestTags.Retry),
                )
            }
            StreamCoreTextButton(
                text = stringResource(Res.string.profile_pin_back),
                onClick = onCancel,
                enabled = true,
                modifier = Modifier.testTag(ProfilePinTestTags.Cancel),
            )
        }
    }
}

@PreviewMobile
@Composable
private fun MobileProfilePinReadyPreview() {
    StreamCoreTheme { MobileProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, draft = "12"), {}, {}, {}) }
}

@PreviewMobile
@Composable
private fun MobileProfilePinLoadingPreview() {
    StreamCoreTheme { MobileProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, isSubmitting = true), {}, {}, {}) }
}

@PreviewMobile
@Composable
private fun MobileProfilePinErrorPreview() {
    StreamCoreTheme { MobileProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, failure = ProfilePinFailure.Incorrect), {}, {}, {}) }
}
