package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCorePlayIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_play_24),
        contentDescription = null,
        colorFilter = ColorFilter.tint(LocalContentColor.current),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StreamCorePlayIconPreview() {
    StreamCoreTheme { StreamCorePlayIcon() }
}
