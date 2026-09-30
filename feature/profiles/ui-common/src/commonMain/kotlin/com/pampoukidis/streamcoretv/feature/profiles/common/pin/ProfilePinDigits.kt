package com.pampoukidis.streamcoretv.feature.profiles.common.pin

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcore.sdk.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.ui.generated.resources.profile_pin_digits_entered
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfilePinDigits(digitCount: Int, enteredDigitCount: Int, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.profile_pin_digits_entered, enteredDigitCount, digitCount)
    Text(
        text = List(digitCount) { if (it < enteredDigitCount) "●" else "○" }.joinToString("  "),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = modifier.testTag(ProfilePinTestTags.Digits).clearAndSetSemantics {
            contentDescription = description
        },
    )
}

@Preview
@Composable
private fun ProfilePinDigitsPreview() {
    StreamCoreTheme { ProfilePinDigits(digitCount = 4, enteredDigitCount = 2) }
}
