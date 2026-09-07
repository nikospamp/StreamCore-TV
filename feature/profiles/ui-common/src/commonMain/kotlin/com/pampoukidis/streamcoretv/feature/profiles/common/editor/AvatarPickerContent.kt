package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCheckIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

/** Shared dialog surface and avatar grid. Callers own dialog placement and input-specific controls. */
@Composable
fun AvatarPickerContent(
    avatars: List<ProfileAvatarModel>,
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    layout: AvatarPickerLayout = AvatarPickerLayout(),
    gridState: LazyGridState = rememberLazyGridState(),
    closeControl: @Composable (onClick: () -> Unit, enabled: Boolean, modifier: Modifier) -> Unit =
        { onClick, enabled, controlModifier ->
            StreamCoreCloseButton(onClick = onClick, enabled = enabled, modifier = controlModifier)
        },
    avatarControl: @Composable (
        avatar: ProfileAvatarModel,
        index: Int,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) -> Unit = { _, _, _, onClick, controlModifier, content ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = controlModifier.clickable(role = Role.RadioButton, onClick = onClick),
        ) {
            content()
        }
    },
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = StreamCoreDimens.Elevation.Medium,
        modifier = modifier.testTag(ProfilesTestTags.EditorAvatarDialog),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier.padding(
                start = StreamCoreDimens.Spacing.Large,
                top = StreamCoreDimens.Spacing.Medium,
                end = StreamCoreDimens.Spacing.Large,
                bottom = StreamCoreDimens.Spacing.ExtraLarge,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Choose an avatar",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                closeControl(onDismissRequest, true, Modifier)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.columns),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(layout.horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(layout.verticalSpacing),
                modifier = Modifier.fillMaxWidth().heightIn(max = layout.gridMaxHeight),
            ) {
                itemsIndexed(
                    items = avatars,
                    key = { _, avatar -> avatar.id },
                    contentType = { _, _ -> AvatarContentType },
                ) { index, avatar ->
                    val selected = avatar.id == selectedAvatarId
                    val semanticLabel = if (selected) "Avatar ${index + 1}, selected" else "Avatar ${index + 1}"
                    avatarControl(
                        avatar,
                        index,
                        selected,
                        { onAvatarSelected(avatar.id) },
                        Modifier.fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = semanticLabel
                                this.selected = selected
                            }
                            .testTag(ProfilesTestTags.EditorAvatarOptionPrefix + avatar.id),
                    ) {
                        AvatarPickerArtwork(avatar = avatar, selected = selected, layout = layout)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarPickerArtwork(avatar: ProfileAvatarModel, selected: Boolean, layout: AvatarPickerLayout) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(StreamCoreDimens.Spacing.Tiny),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = if (selected) {
                BorderStroke(StreamCoreDimens.Stroke.Default, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = Modifier.size(layout.itemSize),
        ) {
            StreamCoreProfileArtwork(avatar = avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        if (selected) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .offset(x = layout.badgeOffset, y = layout.badgeOffset)
                    .size(layout.selectedBadgeSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StreamCoreCheckIcon(modifier = Modifier.size(StreamCoreDimens.Icon.Small))
                }
            }
        }
    }
}

private const val AvatarContentType = "avatar"

@Preview
@Composable
private fun AvatarPickerContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        AvatarPickerContent(
            avatars = ProfilesPreviewData.avatars,
            selectedAvatarId = ProfilesPreviewData.avatars.first().id,
            onAvatarSelected = {},
            onDismissRequest = {},
        )
    }
}
