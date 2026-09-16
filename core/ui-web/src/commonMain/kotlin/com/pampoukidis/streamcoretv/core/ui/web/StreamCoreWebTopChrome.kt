package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Compatibility entry point for existing shell consumers; the browser shell now uses a side rail.
 * The rail's standalone preview is in StreamCoreWebNavigationRail.kt. */
@Deprecated("Use StreamCoreWebNavigationRail")
@Composable
fun StreamCoreWebTopChrome(
    activeDestination: WebBrowseDestination?,
    profileName: String,
    logoutInProgress: Boolean,
    onDestinationSelected: (WebBrowseDestination) -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StreamCoreWebNavigationRail(
        activeDestination = activeDestination,
        profileName = profileName,
        enabled = !logoutInProgress,
        onDestinationSelected = onDestinationSelected,
        onChangeProfile = onChangeProfile,
        modifier = modifier,
    )
}
