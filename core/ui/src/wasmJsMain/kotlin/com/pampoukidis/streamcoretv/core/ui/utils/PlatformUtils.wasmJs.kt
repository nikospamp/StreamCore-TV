package com.pampoukidis.streamcoretv.core.ui.utils

import androidx.compose.runtime.Composable
import com.pampoukidis.streamcoretv.core.model.general.Platform

@Composable
actual fun rememberLoginPlatform(): Platform {
    return Platform.Tv
}
