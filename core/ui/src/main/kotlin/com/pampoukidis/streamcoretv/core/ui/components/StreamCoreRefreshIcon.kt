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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreRefreshIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(R.drawable.ic_refresh_24),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(StreamCoreDimens.Icon.Medium),
    )
}

@Preview
@Composable
private fun StreamCoreRefreshIconPreview() {
    StreamCoreTheme {
        Surface {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(StreamCoreDimens.Icon.TouchTarget),
            ) {
                StreamCoreRefreshIcon()
            }
        }
    }
}