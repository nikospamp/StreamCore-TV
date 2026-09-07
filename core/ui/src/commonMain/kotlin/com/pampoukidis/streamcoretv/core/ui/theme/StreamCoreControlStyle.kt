package com.pampoukidis.streamcoretv.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/** Semantic control decisions. Heights, padding and input/focus mechanics belong to renderers. */
@Immutable
data class StreamCoreControlStyle(
    val primary: Color,
    val onPrimary: Color,
    val pressed: Color,
    val onPressed: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val buttonLabel: TextStyle,
    val buttonRadius: Dp,
    val inputLabel: TextStyle,
    val inputRadius: Dp,
    val disabledInputOpacity: Float = 0.55f,
) {
    val buttonShape: RoundedCornerShape = RoundedCornerShape(buttonRadius)
}

/** Override at a preview/test or application composition boundary; no provider-specific palette. */
val LocalStreamCoreControlStyle = compositionLocalOf<StreamCoreControlStyle?> { null }

object StreamCoreControlDefaults {
    @Composable
    fun style(): StreamCoreControlStyle {
        return LocalStreamCoreControlStyle.current ?: StreamCoreControlStyle(
            primary = MaterialTheme.colorScheme.primary,
            onPrimary = MaterialTheme.colorScheme.onPrimary,
            pressed = MaterialTheme.colorScheme.primaryContainer,
            onPressed = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainer = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            disabledContent = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            buttonLabel = MaterialTheme.typography.labelLarge,
            buttonRadius = StreamCoreButtonRadius,
            inputLabel = MaterialTheme.typography.bodyLarge,
            inputRadius = StreamCoreInputRadius,
        )
    }
}
