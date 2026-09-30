package com.pampoukidis.streamcoretv.feature.profiles.web.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcore.sdk.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.*
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import org.jetbrains.compose.resources.stringResource

@Composable
fun WebProfilePinScreen(
    state: ProfilePinUiState,
    onDraftChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancelFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    var inputFocusRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.isSubmitting, state.failure) {
        if (state.isSubmitting || state.failure == ProfilePinFailure.Locked) cancelFocus.requestFocus()
        else if (state.canRetry) retryFocus.requestFocus()
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize().webEscape(onCancel).testTag(ProfilePinTestTags.Root)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            ProfilePinIdentity(state.profile, StreamCoreDimens.Tv.Profiles.AvatarSize)
            Text(stringResource(Res.string.profile_pin_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.profile_pin_prompt, state.digitCount), textAlign = TextAlign.Center)
            WebProfilePinInput(
                state = state,
                label = stringResource(Res.string.profile_pin_input_label),
                focusRequest = inputFocusRequest + state.inputRevision,
                onDraftChanged = onDraftChanged,
                onCancel = onCancel,
                onTab = { reverse -> if (!reverse && state.canRetry) retryFocus.requestFocus() else cancelFocus.requestFocus() },
                modifier = Modifier.widthIn(max = StreamCoreDimens.Tv.Panel.Width).fillMaxWidth(),
            )
            ProfilePinStatus(state.isSubmitting, state.failure, Modifier.widthIn(max = StreamCoreDimens.Tv.Panel.Width))
            if (state.canRetry) {
                StreamCoreWebButton(
                    text = stringResource(Res.string.profile_pin_retry),
                    onClick = onRetry,
                    modifier = Modifier.focusRequester(retryFocus).testTag(ProfilePinTestTags.Retry).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                            if (event.isShiftPressed) inputFocusRequest++ else cancelFocus.requestFocus()
                            true
                        } else false
                    },
                )
            }
            StreamCoreWebButton(
                text = stringResource(Res.string.profile_pin_back),
                onClick = onCancel,
                variant = StreamCoreWebButtonVariant.Secondary,
                modifier = Modifier.focusRequester(cancelFocus).testTag(ProfilePinTestTags.Cancel).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                        if (state.inputEnabled) {
                            if (event.isShiftPressed && state.canRetry) retryFocus.requestFocus() else inputFocusRequest++
                        }
                        true
                    } else false
                },
            )
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilePinReadyPreview() {
    StreamCoreTheme { WebProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, draft = "12"), {}, {}, {}) }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilePinLoadingPreview() {
    StreamCoreTheme { WebProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, isSubmitting = true), {}, {}, {}) }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilePinErrorPreview() {
    StreamCoreTheme { WebProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, failure = ProfilePinFailure.Incorrect), {}, {}, {}) }
}
