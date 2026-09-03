package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.runtime.Immutable

@Immutable
data class WebBrowseFocusKey(
    val destination: WebBrowseDestination,
    val sectionKey: String,
    val itemKey: String,
) {
    init {
        require(sectionKey.isNotBlank())
        require(itemKey.isNotBlank())
    }
}
