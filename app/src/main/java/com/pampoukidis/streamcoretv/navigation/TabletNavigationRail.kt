package com.pampoukidis.streamcoretv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBrandMark
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHomeIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLibraryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTabletNavigationRail
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTabletNavigationRailItem

@Composable
internal fun TabletNavigationRail(
    selectedDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    StreamCoreTabletNavigationRail(
        modifier = modifier,
        header = { StreamCoreBrandMark() },
    ) {
        TopLevelDestination.entries.forEach { destination ->
            StreamCoreTabletNavigationRailItem(
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
