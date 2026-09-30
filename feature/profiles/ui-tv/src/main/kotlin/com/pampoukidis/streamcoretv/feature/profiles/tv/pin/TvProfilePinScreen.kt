package com.pampoukidis.streamcoretv.feature.profiles.tv.pin

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.pampoukidis.streamcore.sdk.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.*
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import org.jetbrains.compose.resources.stringResource

@Composable
fun TvProfilePinScreen(
    state: ProfilePinUiState,
    onDigitEntered: (Int) -> Unit,
    onDeleteDigit: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keys = remember { List(12) { FocusRequester() } }
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(state.profile.id, state.isSubmitting, state.failure, state.inputRevision) {
        when {
            state.isSubmitting || state.failure == ProfilePinFailure.Locked -> keys[11].requestFocus()
            state.canRetry -> retryFocus.requestFocus()
            else -> keys[0].requestFocus()
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag(ProfilePinTestTags.Root).onPreviewKeyEvent { event ->
            val native = event.nativeKeyEvent
            val code = native.keyCode
            val digit = when (code) {
                in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> code - AndroidKeyEvent.KEYCODE_0
                in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> code - AndroidKeyEvent.KEYCODE_NUMPAD_0
                else -> null
            }
            val handled = digit != null || code == AndroidKeyEvent.KEYCODE_DEL
            if (handled && event.type == KeyEventType.KeyDown && native.repeatCount == 0 && state.inputEnabled) {
                if (digit != null) onDigitEntered(digit) else onDeleteDigit()
            }
            handled
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(StreamCoreDimens.Tv.Screen.HorizontalPadding),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.weight(1f),
            ) {
                ProfilePinIdentity(state.profile, StreamCoreDimens.Tv.Profiles.AvatarSize)
                Text(stringResource(Res.string.profile_pin_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(stringResource(Res.string.profile_pin_prompt, state.digitCount), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                ProfilePinDigits(state.digitCount, state.draft.length)
                ProfilePinStatus(state.isSubmitting, state.failure)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.weight(1f),
            ) {
                repeat(4) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
                        repeat(3) { column ->
                            val index = row * 3 + column
                            val digit = when (index) { in 0..8 -> index + 1; 10 -> 0; else -> null }
                            val label = when {
                                digit != null -> digit.toString()
                                index == 9 -> stringResource(Res.string.profile_pin_delete_digit)
                                else -> stringResource(Res.string.profile_pin_back)
                            }
                            StreamCoreTvButton(
                                text = label,
                                onClick = {
                                    when {
                                        digit != null -> onDigitEntered(digit)
                                        index == 9 -> onDeleteDigit()
                                        else -> onCancel()
                                    }
                                },
                                enabled = index == 11 || state.inputEnabled,
                                variant = if (digit != null) StreamCoreTvButtonVariant.Standard else StreamCoreTvButtonVariant.Tertiary,
                                modifier = Modifier.weight(1f).focusRequester(keys[index]).focusProperties {
                                    left = if (column > 0) keys[index - 1] else FocusRequester.Cancel
                                    right = if (column < 2) keys[index + 1] else FocusRequester.Cancel
                                    up = if (row > 0) keys[index - 3] else FocusRequester.Cancel
                                    down = if (row < 3) keys[index + 3] else if (state.canRetry) retryFocus else FocusRequester.Cancel
                                }.testTag(when {
                                    digit != null -> ProfilePinTestTags.DigitPrefix + digit
                                    index == 9 -> ProfilePinTestTags.Delete
                                    else -> ProfilePinTestTags.Cancel
                                }),
                            )
                        }
                    }
                }
                if (state.canRetry) {
                    StreamCoreTvButton(
                        text = stringResource(Res.string.profile_pin_retry),
                        onClick = onRetry,
                        enabled = true,
                        variant = StreamCoreTvButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth().focusRequester(retryFocus).focusProperties {
                            up = keys[10]
                            down = FocusRequester.Cancel
                        }.testTag(ProfilePinTestTags.Retry),
                    )
                }
            }
        }
    }
}

@PreviewTV
@Composable
private fun TvProfilePinReadyPreview() {
    StreamCoreTheme { TvProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, draft = "12"), {}, {}, {}, {}) }
}

@PreviewTV
@Composable
private fun TvProfilePinLoadingPreview() {
    StreamCoreTheme { TvProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, isSubmitting = true), {}, {}, {}, {}) }
}

@PreviewTV
@Composable
private fun TvProfilePinErrorPreview() {
    StreamCoreTheme { TvProfilePinScreen(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, failure = ProfilePinFailure.Incorrect), {}, {}, {}, {}) }
}
