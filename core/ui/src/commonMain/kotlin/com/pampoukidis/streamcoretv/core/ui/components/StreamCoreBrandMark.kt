package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreBrandMark(modifier: Modifier = Modifier.Companion) {
    val primary = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.size(StreamCoreDimens.Icon.Medium)) {
        drawRoundRect(
            color = foreground,
            cornerRadius = CornerRadius(
                StreamCoreDimens.Spacing.Small.toPx(),
            ),
        )
        drawArc(
            color = primary,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
        )
    }
}

@Preview
@Composable
private fun StreamCoreBrandMarkPreview() {
    StreamCoreTheme {
        Surface {
            StreamCoreBrandMark()
        }
    }
}