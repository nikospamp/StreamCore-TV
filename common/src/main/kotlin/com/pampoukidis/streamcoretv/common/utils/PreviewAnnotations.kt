package com.pampoukidis.streamcoretv.common.utils

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Mobile",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
)
annotation class PreviewMobile

@Preview(
    name = "Tablet",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true,
)
annotation class PreviewTablet

@Preview(
    name = "TV 1080p",
    widthDp = 960,
    heightDp = 540,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
annotation class PreviewTV
