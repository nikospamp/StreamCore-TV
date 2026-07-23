package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreCloseButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(StreamCoreDimens.Icon.TouchTarget),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close_24),
            contentDescription = "Close",
            tint = color,
            modifier = Modifier.size(StreamCoreDimens.Icon.Medium),
        )
    }
}

@Preview
@Composable
private fun StreamCoreCloseButtonPreview() {
    StreamCoreTheme {
        Surface {
            StreamCoreCloseButton(
                onClick = {},
                enabled = true,
            )
        }
    }
}
