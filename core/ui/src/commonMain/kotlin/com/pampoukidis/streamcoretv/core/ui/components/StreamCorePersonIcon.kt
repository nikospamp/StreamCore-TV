package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCorePersonIcon(modifier: Modifier = Modifier) {
    StreamCoreVectorIcon(
        drawableRes = Res.drawable.ic_person_24,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StreamCorePersonIconPreview() {
    StreamCoreTheme {
        StreamCorePersonIcon()
    }
}
