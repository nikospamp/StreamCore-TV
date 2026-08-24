package com.pampoukidis.streamcoretv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pampoukidis.streamcoretv.R
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreFloatingBottomNavigation
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreFloatingBottomNavigationItem
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHomeIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLibraryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon

@Composable
internal fun MobileNavigationBar(
    selectedDestination: MobileTopLevelDestination,
    onDestinationSelected: (MobileTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    StreamCoreFloatingBottomNavigation(
        selectedIndex = selectedDestination.ordinal,
        itemCount = MobileTopLevelDestination.entries.size,
        modifier = modifier,
    ) {
        MobileTopLevelDestination.entries.forEach { destination ->
            StreamCoreFloatingBottomNavigationItem(
                selected = destination == selectedDestination,
                label = stringResource(destination.labelRes),
                onClick = { onDestinationSelected(destination) },
                icon = {
                    when (destination) {
                        MobileTopLevelDestination.Home -> StreamCoreHomeIcon()
                        MobileTopLevelDestination.Search -> StreamCoreSearchIcon()
                        MobileTopLevelDestination.Library -> StreamCoreLibraryIcon()
                    }
                },
            )
        }
    }
}

private val MobileTopLevelDestination.labelRes: Int
    get() {
        return when (this) {
            MobileTopLevelDestination.Home -> R.string.navigation_home
            MobileTopLevelDestination.Search -> R.string.navigation_search
            MobileTopLevelDestination.Library -> R.string.navigation_library
        }
    }
