package com.pampoukidis.streamcoretv.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.rememberDrawerState
import com.pampoukidis.streamcoretv.R
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBrandMark
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHomeIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLibraryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePersonIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.navigationContainer
import com.pampoukidis.streamcoretv.core.ui.extensions.navigationScrim
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreMotionDurations
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

@Composable
internal fun TvNavigationDrawer(
    enabled: Boolean,
    selectedDestination: TopLevelDestination,
    activeProfile: ProfileModel?,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    onProfileSelected: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val focusManager = LocalFocusManager.current
    val contentFocusRequester = remember { FocusRequester() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimBrush = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.navigationScrim,
                MaterialTheme.colorScheme.transparentContainer,
            ),
        ),
        drawerContent = { drawerValue ->
            // Retain the avatar until its shared transition finishes, including when leaving
            // the drawer for Profiles. An immediate conditional removes the source too early.
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = StreamCoreMotionDurations.NavigationEnterMillis,
                        delayMillis = StreamCoreMotionDurations.NavigationEnterDelayMillis,
                    ),
                ),
                exit = fadeOut(
                    animationSpec = tween(StreamCoreMotionDurations.NavigationExitMillis),
                ),
            ) {
                TvDrawerContent(
                    drawerValue = drawerValue,
                    selectedDestination = selectedDestination,
                    activeProfile = activeProfile,
                    enabled = enabled,
                    contentFocusRequester = contentFocusRequester,
                    sharedElementScope = sharedTransitionScope?.let { scope ->
                        StreamCoreSharedElementScope(scope, this)
                    },
                    onDestinationSelected = { destination ->
                        onDestinationSelected(destination)
                        if (destination == selectedDestination) {
                            contentFocusRequester.requestFocus()
                        } else {
                            // The new destination owns its initial focus; don't restore a card
                            // from the outgoing screen while navigation is starting.
                            focusManager.clearFocus()
                        }
                    },
                    onProfileSelected = onProfileSelected,
                )
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(contentFocusRequester)
                    // Nested focus groups (such as lazy lists/rows) restore their own child.
                    .focusRestorer()
                    .focusGroup(),
            ) {
                content()
            }
        },
    )

    // Register after destination content so an open drawer handles Back before the screen.
    BackHandler(enabled = enabled && drawerState.currentValue == DrawerValue.Open) {
        contentFocusRequester.requestFocus()
    }
}

@Composable
private fun TvDrawerContent(
    drawerValue: DrawerValue,
    selectedDestination: TopLevelDestination,
    activeProfile: ProfileModel?,
    enabled: Boolean,
    contentFocusRequester: FocusRequester,
    sharedElementScope: StreamCoreSharedElementScope?,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    onProfileSelected: () -> Unit,
) {
    val expanded = drawerValue == DrawerValue.Open
    val selectedFocusRequester = remember { FocusRequester() }
    val width = if (expanded) {
        StreamCoreDimens.Tv.Navigation.ExpandedWidth
    } else {
        StreamCoreDimens.Tv.Navigation.CollapsedWidth
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.navigationContainer)
            .padding(
                horizontal = StreamCoreDimens.Spacing.Tiny,
                vertical = StreamCoreDimens.Tv.Screen.VerticalPadding,
            )
            .testTag(TvNavigationTestTags.Drawer)
            .focusProperties {
                // Redirect before any child gains focus, not after the nearest item expands
                // the drawer. Intra-drawer Up/Down navigation remains a normal focus search.
                onEnter = {
                    if (enabled) {
                        selectedFocusRequester.requestFocus()
                    } else {
                        cancelFocusChange()
                    }
                }
            }
            .focusGroup(),
    ) {
        TvDrawerBrand(expanded = expanded)
        Spacer(modifier = Modifier.height(StreamCoreDimens.Spacing.ExtraLarge))
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                TvDrawerItem(
                    label = stringResource(destination.labelRes),
                    selected = destination == selectedDestination,
                    expanded = expanded,
                    enabled = enabled,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        when (destination) {
                            TopLevelDestination.Home -> StreamCoreHomeIcon(modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                            TopLevelDestination.Search -> StreamCoreSearchIcon(modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                            TopLevelDestination.Library -> StreamCoreLibraryIcon(modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                        }
                    },
                    modifier = Modifier.testTag(
                        TvNavigationTestTags.destination(destination),
                    ).focusProperties {
                        right = contentFocusRequester
                    }.then(
                        if (destination == selectedDestination) {
                            Modifier.focusRequester(selectedFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TvDrawerItem(
            label = activeProfile?.displayName ?: stringResource(R.string.navigation_profiles),
            selected = false,
            expanded = expanded,
            enabled = enabled,
            onClick = onProfileSelected,
            icon = {
                if (activeProfile == null) {
                    StreamCorePersonIcon(modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                } else {
                    StreamCoreProfileArtwork(
                        avatar = activeProfile.avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(StreamCoreDimens.Tv.Navigation.IconSize)
                            .testTag(TvNavigationTestTags.Avatar)
                            .streamCoreSharedBounds(
                                sharedElementScope = sharedElementScope,
                                key = StreamCoreSharedKey.profileAvatar(activeProfile.id),
                                clipShape = CircleShape,
                            ),
                    )
                }
            },
            modifier = Modifier
                .testTag(TvNavigationTestTags.Profile)
                .focusProperties { right = contentFocusRequester },
        )
    }
}

@Composable
private fun TvDrawerBrand(expanded: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Tv.Navigation.ItemHeight)
            .padding(horizontal = StreamCoreDimens.Spacing.Small),
    ) {
        StreamCoreBrandMark(modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
        if (expanded) {
            Text(
                text = "StreamCore",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TvDrawerItem(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val containerColor = when {
        isFocused -> MaterialTheme.colorScheme.surfaceContainerHighest
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.transparentContainer
    }
    val contentColor = when {
        isFocused || selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = if (isFocused) {
            BorderStroke(
                width = StreamCoreDimens.Tv.Focus.BorderWidth,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Tv.Navigation.ItemHeight)
            .onFocusChanged { focusState -> isFocused = focusState.isFocused }
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
    ) {
        Row(
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = StreamCoreDimens.Spacing.Small),
        ) {
            // The collapsed rail must leave a full square slot after both padding layers.
            // Keep this slot fixed while labels appear/disappear so artwork never compresses.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize),
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    icon()
                }
            }
            if (expanded) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = StreamCoreDimens.Spacing.Medium),
                )
            }
        }
    }
}

internal object TvNavigationTestTags {
    const val Drawer = "tv-navigation:drawer"
    const val Profile = "tv-navigation:profile"
    const val Avatar = "tv-navigation:avatar"

    fun destination(destination: TopLevelDestination): String {
        return "tv-navigation:${destination.name.lowercase()}"
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun TvNavigationDrawerPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TvNavigationDrawer(
                enabled = true,
                selectedDestination = TopLevelDestination.Home,
                activeProfile = ProfilesPreviewData.profiles.first(),
                onDestinationSelected = {},
                onProfileSelected = {},
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
