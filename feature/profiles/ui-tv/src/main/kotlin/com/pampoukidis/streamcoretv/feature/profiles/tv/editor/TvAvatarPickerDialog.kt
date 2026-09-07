package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvActionSurface
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvIconButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerLayout
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

@Composable
internal fun TvAvatarPickerDialog(
    avatars: List<ProfileAvatarModel>,
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TvAvatarPickerContent(
            avatars = avatars,
            selectedAvatarId = selectedAvatarId,
            onAvatarSelected = onAvatarSelected,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .padding(StreamCoreDimens.Tv.Screen.HorizontalPadding)
                .widthIn(max = StreamCoreDimens.Tv.Profiles.AvatarPickerMaxWidth)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun TvAvatarPickerContent(
    avatars: List<ProfileAvatarModel>,
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeFocusRequester = remember { FocusRequester() }
    val selectedFocusRequester = remember { FocusRequester() }
    val selectedIndex = avatars.indexOfFirst { it.id == selectedAvatarId }.coerceAtLeast(0)
    val layout = AvatarPickerLayout(
        gridMaxHeight = StreamCoreDimens.Tv.Profiles.AvatarPickerGridMaxHeight,
        itemSize = StreamCoreDimens.Tv.Profiles.AvatarPickerItemSize,
    )
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = selectedIndex / layout.columns * layout.columns,
    )
    var initialFocusAssigned by remember { mutableStateOf(false) }

    LaunchedEffect(avatars.isEmpty()) {
        if (avatars.isEmpty()) {
            closeFocusRequester.requestFocus()
        }
    }

    AvatarPickerContent(
        avatars = avatars,
        selectedAvatarId = selectedAvatarId,
        onAvatarSelected = onAvatarSelected,
        onDismissRequest = onDismissRequest,
        layout = layout,
        gridState = gridState,
        closeControl = { onClick, enabled, controlModifier ->
            StreamCoreTvIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = controlModifier
                    .focusRequester(closeFocusRequester)
                    .focusProperties {
                        down = if (gridState.layoutInfo.visibleItemsInfo.any { it.index == selectedIndex }) {
                            selectedFocusRequester
                        } else {
                            FocusRequester.Default
                        }
                    }
                    .semantics { contentDescription = "Close" },
            ) {
                StreamCoreCloseIcon()
            }
        },
        avatarControl = { _, index, _, onClick, controlModifier, content ->
            val itemFocusRequester = if (index == selectedIndex) {
                selectedFocusRequester
            } else {
                remember { FocusRequester() }
            }

            // This effect runs only once the selected lazy item is composed and attached.
            LaunchedEffect(index) {
                if (index == selectedIndex && !initialFocusAssigned) {
                    itemFocusRequester.requestFocus()
                    initialFocusAssigned = true
                }
            }

            StreamCoreTvActionSurface(
                onClick = onClick,
                enabled = true,
                role = Role.RadioButton,
                modifier = controlModifier
                    .focusRequester(itemFocusRequester)
                    .focusProperties {
                        if (index < layout.columns) up = closeFocusRequester
                        if (index % layout.columns == 0) left = FocusRequester.Cancel
                        if (index % layout.columns == layout.columns - 1 || index == avatars.lastIndex) {
                            right = FocusRequester.Cancel
                        }
                        if (index + layout.columns >= avatars.size) down = FocusRequester.Cancel
                    },
                content = content,
            )
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun TvAvatarPickerDialogPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                TvAvatarPickerContent(
                    avatars = ProfilesPreviewData.avatars,
                    selectedAvatarId = ProfilesPreviewData.avatars.first().id,
                    onAvatarSelected = {},
                    onDismissRequest = {},
                    modifier = Modifier.widthIn(max = StreamCoreDimens.Tv.Profiles.AvatarPickerMaxWidth),
                )
            }
        }
    }
}
