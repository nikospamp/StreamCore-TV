package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.extensions.navigationContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/**
 * Floating, route-agnostic mobile navigation container with one animated selection capsule.
 *
 * [selectedIndex] and the order of [content] use logical start-to-end ordering. The capsule maps
 * that order to physical coordinates in RTL layouts. Compose's animation clock applies the system
 * motion-duration scale, so the capsule and item colors update immediately when animations are
 * disabled.
 */
@Composable
fun StreamCoreFloatingBottomNavigation(
    selectedIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    require(itemCount > 0) { "itemCount must be greater than zero" }
    require(selectedIndex in 0 until itemCount) {
        "selectedIndex must be within the item count"
    }

    val layoutDirection = LocalLayoutDirection.current
    val animatedLogicalIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(
            durationMillis = SelectionMotionMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "StreamCoreBottomNavigationSelection",
    )
    val capsuleColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.navigationContainer,
        tonalElevation = StreamCoreDimens.Elevation.Low,
        modifier = modifier
            .widthIn(max = StreamCoreDimens.Mobile.Navigation.MaxWidth)
            .fillMaxWidth()
            .height(StreamCoreDimens.Mobile.Navigation.Height),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(StreamCoreDimens.Mobile.Navigation.InnerPadding)
                .drawBehind {
                    val itemWidth = size.width / itemCount
                    val physicalIndex = if (layoutDirection == LayoutDirection.Ltr) {
                        animatedLogicalIndex
                    } else {
                        itemCount - 1f - animatedLogicalIndex
                    }
                    drawRoundRect(
                        color = capsuleColor,
                        topLeft = Offset(x = itemWidth * physicalIndex, y = 0f),
                        size = Size(width = itemWidth, height = size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                content = content,
            )
        }
    }
}

@Composable
fun RowScope.StreamCoreFloatingBottomNavigationItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetIconColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val targetLabelColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = tween(durationMillis = ItemColorMotionMillis),
        label = "StreamCoreBottomNavigationItemIconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = targetLabelColor,
        animationSpec = tween(durationMillis = ItemColorMotionMillis),
        label = "StreamCoreBottomNavigationItemLabelColor",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = StreamCoreDimens.Spacing.Tiny),
    ) {
        CompositionLocalProvider(LocalContentColor provides iconColor) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(StreamCoreDimens.Icon.Standard),
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(StreamCoreDimens.Spacing.Tiny))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val SelectionMotionMillis = 220
private const val ItemColorMotionMillis = 150

@Preview(widthDp = 390)
@Composable
private fun StreamCoreFloatingBottomNavigationPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreamCoreFloatingBottomNavigation(
                selectedIndex = 2,
                itemCount = 3,
                modifier = Modifier.padding(StreamCoreDimens.Mobile.Screen.HorizontalPadding),
            ) {
                StreamCoreFloatingBottomNavigationItem(
                    selected = false,
                    label = "Home",
                    onClick = {},
                    icon = { StreamCoreHomeIcon() },
                )
                StreamCoreFloatingBottomNavigationItem(
                    selected = false,
                    label = "Search",
                    onClick = {},
                    icon = { StreamCoreSearchIcon() },
                )
                StreamCoreFloatingBottomNavigationItem(
                    selected = true,
                    label = "Library",
                    onClick = {},
                    icon = { StreamCoreLibraryIcon() },
                )
            }
        }
    }
}

@Preview(widthDp = 390, locale = "ar")
@Composable
private fun StreamCoreFloatingBottomNavigationRtlPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreamCoreFloatingBottomNavigation(
                selectedIndex = 0,
                itemCount = 3,
                modifier = Modifier.padding(StreamCoreDimens.Mobile.Screen.HorizontalPadding),
            ) {
                StreamCoreFloatingBottomNavigationItem(
                    selected = true,
                    label = "Home",
                    onClick = {},
                    icon = { StreamCoreHomeIcon() },
                )
                StreamCoreFloatingBottomNavigationItem(
                    selected = false,
                    label = "Search",
                    onClick = {},
                    icon = { StreamCoreSearchIcon() },
                )
                StreamCoreFloatingBottomNavigationItem(
                    selected = false,
                    label = "Library",
                    onClick = {},
                    icon = { StreamCoreLibraryIcon() },
                )
            }
        }
    }
}
