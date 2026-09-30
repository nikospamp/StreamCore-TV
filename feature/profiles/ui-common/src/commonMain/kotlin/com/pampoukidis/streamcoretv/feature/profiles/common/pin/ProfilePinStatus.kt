package com.pampoukidis.streamcoretv.feature.profiles.common.pin

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcore.sdk.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfilePinStatus(isSubmitting: Boolean, failure: ProfilePinFailure?, modifier: Modifier = Modifier) {
    val message = when {
        isSubmitting -> stringResource(Res.string.profile_pin_checking)
        failure == ProfilePinFailure.Incorrect -> stringResource(Res.string.profile_pin_incorrect)
        failure == ProfilePinFailure.Unavailable -> stringResource(Res.string.profile_pin_unavailable)
        failure == ProfilePinFailure.Locked -> stringResource(Res.string.profile_pin_locked)
        else -> ""
    }
    Text(
        text = message,
        color = if (failure != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = modifier.testTag(ProfilePinTestTags.Status).semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Preview
@Composable
private fun ProfilePinStatusPreview() {
    StreamCoreTheme { ProfilePinStatus(false, ProfilePinFailure.Incorrect) }
}
