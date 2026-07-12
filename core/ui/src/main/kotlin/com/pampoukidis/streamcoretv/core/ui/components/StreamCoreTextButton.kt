package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(text = text)
    }
}

@Preview
@Composable
private fun StreamCoreTextButtonPreview() {
    StreamCoreTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(StreamCoreDimens.Spacing.Large)
        ) {
            StreamCoreTextButton(
                text = "Forgot password",
                onClick = {},
                enabled = true,
            )
            StreamCoreTextButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )
        }
    }
}