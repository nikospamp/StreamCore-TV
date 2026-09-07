package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.tv.material3.Border
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.painterResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.ic_visibility_off_24
import androidx.compose.material3.LocalContentColor as ComposeLocalContentColor
import androidx.tv.material3.LocalContentColor as TvLocalContentColor

@Composable
fun StreamCoreTvIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val style = StreamCoreControlDefaults.style()
    val shape = remember(style.inputRadius) { InsetIconButtonShape(radius = style.inputRadius) }
    val focusedBorder = Border(
        border = BorderStroke(StreamCoreDimens.Tv.Focus.BorderWidth, style.primary),
        shape = shape,
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(StreamCoreDimens.Icon.TouchTarget),
        scale = IconButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        shape = streamCoreTvButtonShape(shape),
        colors = IconButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = style.pressed,
            focusedContentColor = style.onPressed,
            pressedContainerColor = style.pressed,
            pressedContentColor = style.onPressed,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            disabledContentColor = style.disabledContent,
        ),
        // The compact shape already supplies the inset; TV's positive border inset expands out.
        border = IconButtonDefaults.border(
            focusedBorder = focusedBorder,
            pressedBorder = focusedBorder,
            focusedDisabledBorder = Border(
                border = BorderStroke(
                    width = StreamCoreDimens.Tv.Focus.BorderWidth,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                ),
                shape = shape,
            ),
        ),
    ) {
        CompositionLocalProvider(ComposeLocalContentColor provides TvLocalContentColor.current) {
            content()
        }
    }
}

/** Keeps the 48dp layout/focus bounds while insetting the painted surface around its icon. */
private data class InsetIconButtonShape(val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val inset = with(density) { StreamCoreDimens.Tv.IconButton.SurfaceInset.toPx() }
            .coerceAtMost(size.minDimension / 2f)
        val cornerRadius = with(density) { radius.toPx() }
            .coerceIn(0f, (size.minDimension / 2f - inset).coerceAtLeast(0f))
        return Outline.Rounded(
            RoundRect(
                rect = Rect(inset, inset, size.width - inset, size.height - inset),
                cornerRadius = CornerRadius(cornerRadius),
            ),
        )
    }
}

@Preview
@Composable
private fun StreamCoreTvIconButtonPreview() {
    StreamCoreTheme {
        StreamCoreTvIconButton(
            onClick = {},
            enabled = true,
            modifier = Modifier.padding(StreamCoreDimens.Spacing.Small),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_visibility_off_24),
                contentDescription = "Show password",
            )
        }
    }
}
