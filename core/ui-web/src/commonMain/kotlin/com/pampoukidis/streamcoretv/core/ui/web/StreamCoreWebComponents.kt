package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import kotlinx.coroutines.launch

enum class StreamCoreWebButtonVariant {
    Primary,
    Secondary,
    Tertiary,
    Destructive,
}

@Composable
fun StreamCoreWebButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: StreamCoreWebButtonVariant = StreamCoreWebButtonVariant.Primary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else if (hovered) 1.015f else 1f,
    )
    val elevation by animateDpAsState(
        targetValue = if (focused || hovered) StreamCoreDimens.Elevation.Medium else 0.dp,
    )
    val colors = when (variant) {
        StreamCoreWebButtonVariant.Primary -> ButtonDefaults.buttonColors()
        StreamCoreWebButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        StreamCoreWebButtonVariant.Tertiary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        StreamCoreWebButtonVariant.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    val focusColor = if (focused) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        colors = colors,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = if (focused) StreamCoreWebDimens.FocusBorder else StreamCoreWebDimens.FocusOuterBorder,
            color = if (focused) focusColor else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = elevation,
            focusedElevation = elevation,
            hoveredElevation = elevation,
        ),
        modifier = modifier
            .defaultMinSize(minHeight = StreamCoreWebDimens.ControlHeight)
            .scale(scale)
            .hoverable(interactionSource)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .webSpaceActivation(enabled = enabled && !loading, onClick = onClick)
            .semantics { role = Role.Button },
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = StreamCoreDimens.Stroke.Progress,
                modifier = Modifier.size(StreamCoreWebDimens.ButtonProgressSize),
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun StreamCoreWebPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = StreamCoreDimens.Elevation.Medium,
        modifier = modifier.widthIn(max = StreamCoreWebDimens.PanelWidth),
    ) {
        Box(
            modifier = Modifier.padding(StreamCoreWebDimens.PanelPadding),
        ) {
            content()
        }
    }
}

@Composable
fun StreamCoreWebProfileCard(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else if (hovered) 1.025f else 1f,
    )
    val borderColor = when {
        focused -> MaterialTheme.colorScheme.onBackground
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (focused || hovered) StreamCoreDimens.Elevation.Medium else 0.dp,
        border = BorderStroke(
            width = if (focused) StreamCoreWebDimens.FocusBorder else StreamCoreWebDimens.FocusOuterBorder,
            color = borderColor,
        ),
        modifier = modifier
            .widthIn(min = StreamCoreWebDimens.ProfileCardWidth)
            .scale(scale)
            .hoverable(interactionSource)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .webSpaceActivation(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
    ) {
        Box(content = content)
    }
}

@Composable
fun StreamCoreWebContentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    aspectRatio: Float = 16f / 9f,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else if (hovered) 1.02f else 1f,
    )
    val borderColor = when {
        focused -> MaterialTheme.colorScheme.onBackground
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (focused) StreamCoreWebDimens.FocusBorder else StreamCoreWebDimens.FocusOuterBorder,
            color = borderColor,
        ),
        tonalElevation = if (focused || hovered) StreamCoreDimens.Elevation.Medium else 0.dp,
        modifier = modifier
            .aspectRatio(aspectRatio)
            .scale(scale)
            .hoverable(interactionSource)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .webSpaceActivation(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
    ) {
        Box(content = content)
    }
}

@Composable
fun StreamCoreWebLargeScreenBackground(
    modifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit = {},
    scrim: @Composable BoxScope.() -> Unit = { StreamCoreWebScrim() },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        artwork()
        scrim()
        content()
    }
}

@Composable
fun BoxScope.StreamCoreWebScrim(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .matchParentSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.82f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                    ),
                ),
            ),
    )
}

@Composable
fun StreamCoreWebBlockingSurface(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        StreamCoreWebPanel {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(StreamCoreWebDimens.BlockingProgressSize),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionLabel != null && onAction != null) {
                    StreamCoreWebButton(
                        text = actionLabel,
                        onClick = onAction,
                    )
                }
            }
        }
    }
}

fun Modifier.webEscape(onEscape: () -> Unit): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            onEscape()
            true
        } else {
            false
        }
    }
}

private fun Modifier.webSpaceActivation(
    enabled: Boolean,
    onClick: () -> Unit,
): Modifier {
    return onPreviewKeyEvent { event ->
        if (enabled && event.type == KeyEventType.KeyUp && event.key == Key.Spacebar) {
            onClick()
            true
        } else {
            false
        }
    }
}

@Preview
@Composable
private fun StreamCoreWebButtonPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge)) {
            StreamCoreWebButton(text = "Continue", onClick = {})
        }
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun StreamCoreWebContentCardPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebLargeScreenBackground(
            content = {
                StreamCoreWebContentCard(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = StreamCoreWebDimens.PanelWidth),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Text("Content artwork")
                    }
                }
            },
        )
    }
}
