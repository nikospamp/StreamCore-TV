package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.tooling.preview.Preview
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreInfoIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(Res.drawable.ic_outline_info_24),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(StreamCoreDimens.Icon.Medium),
    )
}

@Preview
@Composable
private fun StreamCoreInfoIconPreview() {
    StreamCoreTheme {
        Surface {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(StreamCoreDimens.Icon.TouchTarget),
            ) {
                StreamCoreInfoIcon()
            }
        }
    }
}