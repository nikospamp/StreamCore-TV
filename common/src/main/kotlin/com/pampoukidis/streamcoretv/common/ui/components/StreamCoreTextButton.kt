package com.pampoukidis.streamcoretv.common.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.common.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.common.utils.PreviewMobile

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
    StreamCoreTVTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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