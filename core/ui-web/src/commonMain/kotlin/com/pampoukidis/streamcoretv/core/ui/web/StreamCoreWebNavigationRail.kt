package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBrandMark
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHomeIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLibraryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePersonIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.navigationContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** TV visual vocabulary with browser pointer, Tab, and arrow-key interaction. */
@Composable
fun StreamCoreWebNavigationRail(
    activeDestination: WebBrowseDestination?,
    profileName: String,
    onDestinationSelected: (WebBrowseDestination) -> Unit,
    onChangeProfile: () -> Unit,
    modifier: Modifier = Modifier,
    profile: ProfileModel? = null,
    enabled: Boolean = true,
    allowExpansion: Boolean = true,
    onMoveToContent: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var containsFocus by remember { mutableStateOf(false) }
    val expanded = allowExpansion && (hovered || containsFocus)
    val destinations = remember { listOf(WebBrowseDestination.Home, WebBrowseDestination.Search, WebBrowseDestination.Library) }
    val focusTargets = remember { List(4) { FocusRequester() } }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(if (expanded) StreamCoreDimens.Tv.Navigation.ExpandedWidth else StreamCoreDimens.Tv.Navigation.CollapsedWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.navigationContainer)
            .hoverable(interaction)
            .onFocusChanged { containsFocus = it.hasFocus }
            .focusGroup()
            .padding(horizontal = StreamCoreDimens.Spacing.Tiny, vertical = StreamCoreDimens.Tv.Screen.VerticalPadding)
            .testTag("web-navigation-rail"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().height(StreamCoreDimens.Tv.Navigation.ItemHeight)
                .padding(horizontal = StreamCoreDimens.Spacing.Small),
        ) {
            StreamCoreBrandMark(modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
            if (expanded) {
                Text("StreamCore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(StreamCoreDimens.Spacing.ExtraLarge))
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            destinations.forEachIndexed { index, destination ->
                val label = when (destination) {
                    WebBrowseDestination.Home -> "Home"
                    WebBrowseDestination.Search -> "Search"
                    WebBrowseDestination.Library -> "Library"
                    WebBrowseDestination.Details -> "Details"
                }
                NavigationItem(
                    label = label,
                    selected = activeDestination == destination,
                    expanded = expanded,
                    enabled = enabled,
                    onClick = { onDestinationSelected(destination) },
                    modifier = Modifier.focusRequester(focusTargets[index])
                        .railKeys(focusTargets.getOrNull(index - 1), focusTargets[index + 1], onMoveToContent)
                        .testTag("web-navigation-" + label.lowercase()),
                ) {
                    when (destination) {
                        WebBrowseDestination.Home -> StreamCoreHomeIcon(Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                        WebBrowseDestination.Search -> StreamCoreSearchIcon(Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                        WebBrowseDestination.Library -> StreamCoreLibraryIcon(Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
                        WebBrowseDestination.Details -> Unit
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        NavigationItem(
            label = profile?.displayName ?: profileName,
            accessibilityLabel = "Change profile",
            selected = false,
            expanded = expanded,
            enabled = enabled,
            onClick = onChangeProfile,
            modifier = Modifier.focusRequester(focusTargets.last())
                .railKeys(focusTargets[2], null, onMoveToContent)
                .testTag("web-navigation-profile"),
        ) {
            if (profile == null) {
                StreamCorePersonIcon(Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize))
            } else {
                StreamCoreProfileArtwork(
                    avatar = profile.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(StreamCoreDimens.Tv.Navigation.IconSize).clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = label,
    icon: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    StreamCoreWebActionSurface(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        contentColor = if (selected || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().height(StreamCoreDimens.Tv.Navigation.ItemHeight)
            .onFocusChanged { focused = it.isFocused }
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
                this.selected = selected
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier.fillMaxWidth().padding(horizontal = StreamCoreDimens.Spacing.Medium),
        ) {
            icon()
            if (expanded) {
                Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun Modifier.railKeys(up: FocusRequester?, down: FocusRequester?, onMoveToContent: (() -> Unit)?): Modifier {
    return focusProperties {
        if (up != null) this.up = up
        if (down != null) this.down = down
    }.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
            event.key == Key.DirectionUp && up != null -> { up.requestFocus(); true }
            event.key == Key.DirectionDown && down != null -> { down.requestFocus(); true }
            (event.key == Key.DirectionRight || event.key == Key.Escape) && onMoveToContent != null -> { onMoveToContent(); true }
            else -> false
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun StreamCoreWebNavigationRailPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebNavigationRail(
            activeDestination = WebBrowseDestination.Home,
            profileName = "Nikos",
            onDestinationSelected = {},
            onChangeProfile = {},
        )
    }
}
