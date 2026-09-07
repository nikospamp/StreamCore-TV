package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTvButtonMaxRadius
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
import androidx.tv.material3.ButtonColors
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
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
        enabled = enabled && !loading,
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
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { if (loading) contentDescription = text },
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
                    Text(text = text, style = StreamCoreControlDefaults.style().buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun streamCoreTvButtonColors(
    variant: StreamCoreTvButtonVariant,
    selected: Boolean,
): ButtonColors {
    val style = StreamCoreControlDefaults.style()
    return when (variant) {
        StreamCoreTvButtonVariant.Standard,
        StreamCoreTvButtonVariant.Primary -> ButtonDefaults.colors(
            containerColor = style.primary,
            contentColor = style.onPrimary,
            focusedContainerColor = style.primary,
            focusedContentColor = style.onPrimary,
            pressedContainerColor = style.pressed,
            pressedContentColor = style.onPressed,
            disabledContainerColor = style.disabledContainer,
            disabledContentColor = style.disabledContent,
        )

        StreamCoreTvButtonVariant.Secondary -> ButtonDefaults.colors(
            containerColor = if (selected) {
                style.pressed
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (selected) {
                style.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            focusedContainerColor = if (selected) {
                style.pressed
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            focusedContentColor = if (selected) {
                style.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            pressedContainerColor = style.pressed,
            pressedContentColor = style.onPressed,
            disabledContainerColor = style.disabledContainer,
            disabledContentColor = style.disabledContent,
        )

        StreamCoreTvButtonVariant.Tertiary -> ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            contentColor = MaterialTheme.colorScheme.onBackground,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            pressedContainerColor = style.pressed,
            pressedContentColor = style.onPressed,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            disabledContentColor = style.disabledContent,
        )
    }
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
private fun streamCoreTvButtonCornerShape(): RoundedCornerShape {
    return RoundedCornerShape(minOf(StreamCoreControlDefaults.style().buttonRadius, StreamCoreTvButtonMaxRadius))
}

@Composable
private fun streamCoreTvButtonShape(): ButtonShape {
    val shape = streamCoreTvButtonCornerShape()
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
            color = StreamCoreControlDefaults.style().primary,
        ),
        inset = StreamCoreDimens.Tv.Focus.BorderPadding,
        shape = streamCoreTvButtonCornerShape(),
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
        shape = streamCoreTvButtonCornerShape(),
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
