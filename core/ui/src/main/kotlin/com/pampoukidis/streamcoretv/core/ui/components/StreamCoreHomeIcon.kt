package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreHomeIcon(modifier: Modifier = Modifier) {
    StreamCoreVectorIcon(
        drawableRes = R.drawable.ic_home_24,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StreamCoreHomeIconPreview() {
    StreamCoreTheme { StreamCoreHomeIcon() }
}
