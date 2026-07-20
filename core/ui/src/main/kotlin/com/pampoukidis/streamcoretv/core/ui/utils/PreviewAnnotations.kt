package com.pampoukidis.streamcoretv.core.ui.utils

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
    name = "Mobile Tall",
    widthDp = 390,
    heightDp = 1200,
    showBackground = true,
)
annotation class PreviewMobileTall

@Preview(
    name = "Tablet",
    widthDp = 1024,
    heightDp = 1400,
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
