package com.pampoukidis.streamcoretv.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val StreamCoreButtonRadius = 999.dp
internal val StreamCoreInputRadius = 6.dp
internal val StreamCoreTvButtonMaxRadius = 8.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

internal val StreamCoreBottomRoundedShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomEnd = 16.dp,
    bottomStart = 16.dp,
)
