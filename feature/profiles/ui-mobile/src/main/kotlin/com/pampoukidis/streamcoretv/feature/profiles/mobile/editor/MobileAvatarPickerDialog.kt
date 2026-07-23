package com.pampoukidis.streamcoretv.feature.profiles.mobile.editor

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCheckIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileAvatar
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
internal fun MobileAvatarPickerDialog(
    avatars: List<ProfileAvatarModel>,
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = StreamCoreDimens.Elevation.Medium,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = AvatarPickerMaxWidth)
                .testTag(ProfilesTestTags.EditorAvatarDialog),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Choose an avatar",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    StreamCoreCloseButton(
                        onClick = onDismissRequest,
                        enabled = true,
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(AvatarPickerColumnCount),
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = AvatarPickerGridMaxHeight),
                ) {
                    itemsIndexed(
                        items = avatars,
                        key = { _, avatar -> avatar.id },
                        contentType = { _, _ -> AvatarContentType },
                    ) { index, avatar ->
                        AvatarPickerItem(
                            avatar = avatar,
                            avatarIndex = index,
                            selected = avatar.id == selectedAvatarId,
                            onClick = { onAvatarSelected(avatar.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarPickerItem(
    avatar: ProfileAvatarModel,
    avatarIndex: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fallbackAvatar = remember(avatar.id) {
        StreamCoreProfileAvatar.fallbackFor(avatar.id)
    }
    val semanticLabel = if (selected) {
        "Avatar ${avatarIndex + 1}, selected"
    } else {
        "Avatar ${avatarIndex + 1}"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = semanticLabel
                this.selected = selected
            }
            .clip(CircleShape)
            .clickable(
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(StreamCoreDimens.Spacing.Tiny)
            .testTag(ProfilesTestTags.EditorAvatarOptionPrefix + avatar.id),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = if (selected) {
                BorderStroke(StreamCoreDimens.Stroke.Default, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = Modifier.size(AvatarPickerItemSize),
        ) {
            StreamCoreProfileArtwork(
                imageUrl = avatar.imageUrl,
                fallbackAvatar = fallbackAvatar,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (selected) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = SelectedBadgeOffset, y = SelectedBadgeOffset)
                    .size(SelectedBadgeSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StreamCoreCheckIcon(modifier = Modifier.size(StreamCoreDimens.Icon.Small))
                }
            }
        }
    }
}

@Preview
@Composable
private fun MobileAvatarPickerDialogPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileAvatarPickerDialog(
            avatars = ProfilesPreviewData.avatars,
            selectedAvatarId = ProfilesPreviewData.avatars.first().id,
            onAvatarSelected = {},
            onDismissRequest = {},
        )
    }
}

private const val AvatarPickerColumnCount = 4
private const val AvatarContentType = "avatar"
private val AvatarPickerMaxWidth = 400.dp
private val AvatarPickerGridMaxHeight = 420.dp
private val AvatarPickerItemSize = 64.dp
private val SelectedBadgeSize = 22.dp
private val SelectedBadgeOffset = 2.dp
