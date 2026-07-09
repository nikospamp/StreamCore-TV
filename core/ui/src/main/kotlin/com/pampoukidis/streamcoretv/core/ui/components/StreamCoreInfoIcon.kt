package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile

@Composable
fun StreamCoreInfoIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        drawCircle(
            color = color,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.5f, size.height * 0.45f),
            end = Offset(size.width * 0.5f, size.height * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = strokeWidth * 0.55f,
            center = Offset(size.width * 0.5f, size.height * 0.29f),
        )
    }
}

@PreviewMobile
@Composable
private fun StreamCoreInfoIconPreview() {
    StreamCoreTVTheme {
        Surface {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp),
            ) {
                StreamCoreInfoIcon()
            }
        }
    }
}
