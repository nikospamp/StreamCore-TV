package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

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
    val destinations = remember { TopLevelDestinations }
    val destinationFocus = remember {
        destinations.associateWith { FocusRequester() }
    }
    val changeProfileFocus = remember { FocusRequester() }
    val logoutFocus = remember { FocusRequester() }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = StreamCoreDimens.Elevation.Low,
        modifier = modifier
            .fillMaxWidth()
            .height(StreamCoreWebDimens.TopChromeHeight),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = StreamCoreWebDimens.ScreenHorizontal),
        ) {
            Text(
                text = "StreamCoreTV",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = StreamCoreDimens.Spacing.Large),
            )
            destinations.forEachIndexed { index, destination ->
                val previous = destinations.getOrNull(index - 1)?.let(destinationFocus::get)
                val next = destinations.getOrNull(index + 1)?.let(destinationFocus::get)
                    ?: changeProfileFocus
                StreamCoreWebButton(
                    text = destination.label(),
                    onClick = { onDestinationSelected(destination) },
                    variant = if (destination == activeDestination) {
                        StreamCoreWebButtonVariant.Secondary
                    } else {
                        StreamCoreWebButtonVariant.Tertiary
                    },
                    modifier = Modifier
                        .focusRequester(requireNotNull(destinationFocus[destination]))
                        .focusProperties {
                            if (previous != null) left = previous
                            right = next
                        }
                        .onPreviewKeyEvent { event ->
                            when {
                                event.type != KeyEventType.KeyDown -> false
                                event.key == Key.DirectionLeft && previous != null -> {
                                    previous.requestFocus()
                                    true
                                }
                                event.key == Key.DirectionRight -> {
                                    next.requestFocus()
                                    true
                                }
                                else -> false
                            }
                        }
                        .semantics { selected = destination == activeDestination },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = profileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.widthIn(max = StreamCoreWebDimens.TopChromeProfileWidth),
            )
            StreamCoreWebButton(
                text = "Change profile",
                onClick = onChangeProfile,
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier
                    .focusRequester(changeProfileFocus)
                    .focusProperties {
                        left = requireNotNull(destinationFocus[destinations.last()])
                        right = logoutFocus
                    }
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.DirectionLeft -> {
                                requireNotNull(destinationFocus[destinations.last()]).requestFocus()
                                true
                            }
                            event.key == Key.DirectionRight -> {
                                logoutFocus.requestFocus()
                                true
                            }
                            else -> false
                        }
                    },
            )
            StreamCoreWebButton(
                text = "Sign out",
                onClick = onLogout,
                enabled = !logoutInProgress,
                loading = logoutInProgress,
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier
                    .focusRequester(logoutFocus)
                    .focusProperties { left = changeProfileFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                            changeProfileFocus.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
            )
        }
    }
}

private fun WebBrowseDestination.label(): String {
    return when (this) {
        WebBrowseDestination.Home -> "Home"
        WebBrowseDestination.Search -> "Search"
        WebBrowseDestination.Library -> "Library"
        WebBrowseDestination.Details -> "Details"
    }
}

private val TopLevelDestinations = listOf(
    WebBrowseDestination.Home,
    WebBrowseDestination.Search,
    WebBrowseDestination.Library,
)

@Preview(widthDp = 1280, heightDp = 160)
@Composable
private fun StreamCoreWebTopChromePreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebTopChrome(
            activeDestination = WebBrowseDestination.Home,
            profileName = "Nikos",
            logoutInProgress = false,
            onDestinationSelected = {},
            onChangeProfile = {},
            onLogout = {},
        )
    }
}
