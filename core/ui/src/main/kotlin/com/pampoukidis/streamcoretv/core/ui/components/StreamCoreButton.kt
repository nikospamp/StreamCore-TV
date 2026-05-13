package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile

@Composable
fun StreamCoreButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = StreamCoreDimens.Button.MinHeight),
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = StreamCoreDimens.Button.LoadingIndicatorStrokeWidth,
                modifier = Modifier.size(StreamCoreDimens.Button.LoadingIndicatorSize),
            )
        } else {
            Text(text = text)
        }
    }
}

@Preview
@Composable
private fun StreamCoreButtonPreview() {
    StreamCoreTVTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StreamCoreButton(
                text = "Continue",
                onClick = {},
                enabled = true,
            )
            StreamCoreButton(
                text = "Loading",
                onClick = {},
                enabled = false,
                loading = true,
            )
            StreamCoreButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )
        }
    }
}
