package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens

@Composable
internal fun StreamCoreVectorIcon(
    drawableRes: Int,
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
