package com.pampoukidis.streamcoretv.core.ui.extensions

import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreBottomRoundedShape

/** Shape with square top corners and rounded bottom corners. */
val Shapes.bottomRounded: Shape
    get() {
        return StreamCoreBottomRoundedShape
    }
