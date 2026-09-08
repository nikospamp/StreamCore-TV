package com.pampoukidis.streamcoretv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.R
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreFloatingBottomNavigation
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreFloatingBottomNavigationItem
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHomeIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLibraryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** Shared floating navigation for phone and tablet touch surfaces. */
@Composable
internal fun MobileNavigationBar(
    selectedDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    StreamCoreFloatingBottomNavigation(
        selectedIndex = selectedDestination.ordinal,
        itemCount = TopLevelDestination.entries.size,
        modifier = modifier,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            StreamCoreFloatingBottomNavigationItem(
                selected = destination == selectedDestination,
                label = stringResource(destination.labelRes),
                onClick = { onDestinationSelected(destination) },
                icon = {
                    when (destination) {
                        TopLevelDestination.Home -> StreamCoreHomeIcon()
                        TopLevelDestination.Search -> StreamCoreSearchIcon()
                        TopLevelDestination.Library -> StreamCoreLibraryIcon()
                    }
                },
            )
        }
    }
}

internal val TopLevelDestination.labelRes: Int
    get() {
        return when (this) {
            TopLevelDestination.Home -> R.string.navigation_home
            TopLevelDestination.Search -> R.string.navigation_search
            TopLevelDestination.Library -> R.string.navigation_library
        }
    }

@Preview
@Composable
private fun MobileNavigationBarPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileNavigationBar(
            selectedDestination = TopLevelDestination.Home,
            onDestinationSelected = {},
        )
    }
}
