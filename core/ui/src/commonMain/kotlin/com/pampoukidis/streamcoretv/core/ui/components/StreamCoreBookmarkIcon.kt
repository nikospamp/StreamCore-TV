package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreBookmarkIcon(
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    StreamCoreVectorIcon(
        drawableRes = if (filled) {
            Res.drawable.ic_bookmark_filled_24
        } else {
            Res.drawable.ic_bookmark_outline_24
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StreamCoreBookmarkIconPreview() {
    StreamCoreTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            StreamCoreBookmarkIcon(filled = false)
            StreamCoreBookmarkIcon(filled = true)
        }
    }
}
