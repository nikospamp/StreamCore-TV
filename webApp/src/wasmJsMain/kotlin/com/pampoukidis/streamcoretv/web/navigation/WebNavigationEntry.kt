package com.pampoukidis.streamcoretv.web.navigation

import androidx.compose.runtime.Immutable
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey

@Immutable
internal data class WebNavigationEntry(
    val route: WebRoute,
    val returnFocusKey: WebBrowseFocusKey? = null,
)
