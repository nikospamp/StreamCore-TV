package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun StreamCoreVectorIcon(
    drawableRes: DrawableResource,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(drawableRes),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(StreamCoreDimens.Icon.Standard),
    )
}
