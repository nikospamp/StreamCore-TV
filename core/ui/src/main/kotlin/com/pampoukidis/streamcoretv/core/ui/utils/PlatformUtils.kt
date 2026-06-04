package com.pampoukidis.streamcoretv.core.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.pampoukidis.streamcoretv.core.model.general.Platform

@Composable
fun rememberLoginPlatform(): Platform {
    val configuration = LocalConfiguration.current
    val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK

    return when {
        uiModeType == Configuration.UI_MODE_TYPE_TELEVISION -> Platform.Tv
        configuration.screenWidthDp >= TabletMinWidthDp -> Platform.Tablet
        else -> Platform.Mobile
    }
}

private const val TabletMinWidthDp = 600
