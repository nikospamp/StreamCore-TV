package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.Text
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import androidx.compose.material3.LocalContentColor as ComposeLocalContentColor
import androidx.tv.material3.LocalContentColor as TvLocalContentColor

@Composable
fun StreamCoreTvButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    variant: StreamCoreTvButtonVariant = StreamCoreTvButtonVariant.Standard,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        enabled = enabled,
        scale = streamCoreTvButtonScale(),
        shape = streamCoreTvButtonShape(),
        colors = streamCoreTvButtonColors(
            variant = variant,
            selected = selected,
        ),
        border = streamCoreTvButtonBorder(),
        tonalElevation = when {
            variant == StreamCoreTvButtonVariant.Standard -> 0.dp
            isFocused -> StreamCoreDimens.Elevation.Medium
            else -> StreamCoreDimens.Elevation.Low
        },
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
    ) {
        CompositionLocalProvider(ComposeLocalContentColor provides TvLocalContentColor.current) {
            if (loading) {
                CircularProgressIndicator(
                    color = TvLocalContentColor.current,
                    strokeWidth = StreamCoreDimens.Button.LoadingIndicatorStrokeWidth,
                    modifier = Modifier.size(StreamCoreDimens.Button.LoadingIndicatorSize),
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon?.invoke()
                    Text(text = text)
                }
            }
        }
    }
}

@Composable
private fun streamCoreTvButtonColors(
    variant: StreamCoreTvButtonVariant,
    selected: Boolean,
) = when (variant) {
    StreamCoreTvButtonVariant.Standard -> ButtonDefaults.colors()

    StreamCoreTvButtonVariant.Primary -> ButtonDefaults.colors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        focusedContainerColor = MaterialTheme.colorScheme.primary,
        focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        pressedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        pressedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    )

    StreamCoreTvButtonVariant.Secondary -> ButtonDefaults.colors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        focusedContainerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        focusedContentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        pressedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        pressedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    )

    StreamCoreTvButtonVariant.Tertiary -> ButtonDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        contentColor = MaterialTheme.colorScheme.onBackground,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        focusedContentColor = MaterialTheme.colorScheme.onSurface,
        pressedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        pressedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun streamCoreTvButtonBorder(): ButtonBorder = ButtonDefaults.border(
    focusedBorder = focusedButtonBorder(),
    pressedBorder = focusedButtonBorder(),
    focusedDisabledBorder = disabledFocusedButtonBorder(),
)

private fun streamCoreTvButtonScale(): ButtonScale = ButtonDefaults.scale(
    focusedScale = 1f,
    pressedScale = 1f,
)

@Composable
private fun streamCoreTvButtonShape(): ButtonShape {
    val shape = MaterialTheme.shapes.small
    return ButtonDefaults.shape(
        shape = shape,
        focusedShape = shape,
        pressedShape = shape,
        disabledShape = shape,
        focusedDisabledShape = shape,
    )
}

@Composable
private fun focusedButtonBorder(): Border {
    return Border(
        border = BorderStroke(
            width = StreamCoreDimens.Tv.Focus.BorderWidth,
            color = MaterialTheme.colorScheme.primary,
        ),
        inset = StreamCoreDimens.Tv.Focus.BorderPadding,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun disabledFocusedButtonBorder(): Border {
    return Border(
        border = BorderStroke(
            width = StreamCoreDimens.Tv.Focus.BorderWidth,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        ),
        inset = StreamCoreDimens.Tv.Focus.BorderPadding,
        shape = MaterialTheme.shapes.small,
    )
}

@Preview
@Composable
private fun StreamCoreTvButtonPreview() {
    StreamCoreTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
        ) {
            StreamCoreTvButton(
                text = "Continue",
                onClick = {},
                enabled = true,
            )
            StreamCoreTvButton(
                text = "My List",
                onClick = {},
                enabled = true,
                variant = StreamCoreTvButtonVariant.Secondary,
                selected = true,
            )
            StreamCoreTvButton(
                text = "Loading",
                onClick = {},
                enabled = false,
                loading = true,
            )
            StreamCoreTvButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )
            StreamCoreTvButton(
                text = "Manage",
                onClick = {},
                enabled = true,
                variant = StreamCoreTvButtonVariant.Tertiary,
            )
        }
    }
}
