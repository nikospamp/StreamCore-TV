package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** Route-agnostic tablet navigation surface used by the app navigation layer. */
@Composable
fun StreamCoreTabletNavigationRail(
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRail(
        header = header,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxHeight()
            .selectableGroup()
            .testTag(StreamCoreTabletNavigationTestTags.Rail),
        content = content,
    )
}

@Composable
fun ColumnScope.StreamCoreTabletNavigationRailItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(text = label, maxLines = 1) },
        alwaysShowLabel = true,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.semantics { role = Role.Tab },
    )
}

@Preview(widthDp = 720, heightDp = 540)
@Composable
private fun StreamCoreTabletNavigationRailPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreamCoreTabletNavigationRail(
                header = {
                    StreamCoreBrandMark(
                        modifier = Modifier.padding(StreamCoreDimens.Spacing.Medium),
                    )
                },
            ) {
                StreamCoreTabletNavigationRailItem(
                    selected = true,
                    label = "Home",
                    onClick = {},
                    icon = { StreamCoreHomeIcon() },
                )
                StreamCoreTabletNavigationRailItem(
                    selected = false,
                    label = "Search",
                    onClick = {},
                    icon = { StreamCoreSearchIcon() },
                )
                StreamCoreTabletNavigationRailItem(
                    selected = false,
                    label = "Library",
                    onClick = {},
                    icon = { StreamCoreLibraryIcon() },
                )
            }
        }
    }
}
