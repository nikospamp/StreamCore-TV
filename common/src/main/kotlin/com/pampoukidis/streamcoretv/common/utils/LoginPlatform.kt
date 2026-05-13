package com.pampoukidis.streamcoretv.common.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class LoginPlatform {
    Mobile,
    Tablet,
    Tv,
}

@Composable
fun rememberLoginPlatform(): LoginPlatform {
    val configuration = LocalConfiguration.current
    val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK

    return when {
        uiModeType == Configuration.UI_MODE_TYPE_TELEVISION -> LoginPlatform.Tv
        configuration.screenWidthDp >= TabletMinWidthDp -> LoginPlatform.Tablet
        else -> LoginPlatform.Mobile
    }
}

private const val TabletMinWidthDp = 600
