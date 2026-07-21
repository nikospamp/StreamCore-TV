package com.pampoukidis.streamcoretv.core.ui.extensions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val BottomRoundedShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomEnd = 16.dp,
    bottomStart = 16.dp,
)

/** Shape with square top corners and rounded bottom corners. */
val Shapes.bottomRounded: Shape
    get() {
        return BottomRoundedShape
    }
