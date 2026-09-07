package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    size: StreamCoreButtonSize = StreamCoreButtonSize.Standard,
    variant: StreamCoreButtonVariant = StreamCoreButtonVariant.Primary,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val minHeight = when (size) {
        StreamCoreButtonSize.Standard -> StreamCoreDimens.Button.MinHeight
        StreamCoreButtonSize.Compact -> StreamCoreDimens.Button.CompactHeight
    }
    val contentPadding = when (size) {
        StreamCoreButtonSize.Standard -> ButtonDefaults.ContentPadding
        StreamCoreButtonSize.Compact -> PaddingValues(
            horizontal = StreamCoreDimens.Button.CompactHorizontalPadding,
        )
    }
    val style = StreamCoreControlDefaults.style()
    val colors = when (variant) {
        StreamCoreButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = style.primary,
            contentColor = style.onPrimary,
            disabledContainerColor = style.disabledContainer,
            disabledContentColor = style.disabledContent,
        )
        StreamCoreButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = style.disabledContainer,
            disabledContentColor = style.disabledContent,
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = colors,
        shape = style.buttonShape,
        contentPadding = contentPadding,
        modifier = modifier.defaultMinSize(minHeight = minHeight)
            .semantics { if (loading) contentDescription = text },
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                strokeWidth = StreamCoreDimens.Button.LoadingIndicatorStrokeWidth,
                modifier = Modifier.size(StreamCoreDimens.Button.LoadingIndicatorSize),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.invoke()
                Text(text = text, style = style.buttonLabel)
            }
        }
    }
}

@Preview
@Composable
private fun StreamCoreButtonPreview() {
    StreamCoreTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(StreamCoreDimens.Spacing.Large),
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
