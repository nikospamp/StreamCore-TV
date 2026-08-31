package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreAddIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
internal fun TvAddProfileTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.06f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "tv-add-profile-focus",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .size(
                width = StreamCoreDimens.Tv.Profiles.TileWidth,
                height = StreamCoreDimens.Tv.Profiles.TileHeight,
            )
            .scale(scale)
            .semantics(mergeDescendants = true) {
                contentDescription = "Add profile"
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .testTag(ProfilesTestTags.AddProfileButton),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(StreamCoreDimens.Tv.Profiles.FocusContainerSize),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isFocused) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    width = if (isFocused) StreamCoreDimens.Tv.Focus.BorderWidth else 0.dp,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.background
                    },
                ),
                tonalElevation = if (isFocused) {
                    StreamCoreDimens.Elevation.Medium
                } else {
                    0.dp
                },
                modifier = Modifier.size(StreamCoreDimens.Tv.Profiles.AvatarSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StreamCoreAddIcon(
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(StreamCoreDimens.Icon.Large),
                    )
                }
            }
        }
        Text(
            text = "Add profile",
            style = MaterialTheme.typography.titleLarge,
            color = if (isFocused) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun TvAddProfileTilePreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            TvAddProfileTile(
                enabled = true,
                onClick = {},
            )
        }
    }
}
