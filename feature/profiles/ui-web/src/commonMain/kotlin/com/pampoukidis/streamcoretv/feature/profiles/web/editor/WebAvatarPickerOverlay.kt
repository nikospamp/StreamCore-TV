package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebActionSurface
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerLayout
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

@Composable
internal fun WebAvatarPickerOverlay(
    avatars: List<ProfileAvatarModel>,
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    WebAvatarPickerEscapeEffect(onDismissRequest)
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(avatars.isEmpty()) {
        if (avatars.isEmpty()) closeFocus.requestFocus()
    }
    val selectedIndex = avatars.indexOfFirst { it.id == selectedAvatarId }.coerceAtLeast(0)
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = selectedIndex / 4 * 4)
    var initialFocusAssigned by remember { mutableStateOf(false) }
    // Stay in the host Compose viewport: a separate browser Dialog can retain its accessibility root.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
            .webEscape(onDismissRequest)
            .semantics {
                paneTitle = "Choose an avatar"
                isTraversalGroup = true
            },
    ) {
        Box(
            modifier = Modifier.matchParentSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = AvatarPickerScrimOpacity))
                .pointerInput(onDismissRequest) {
                    detectTapGestures(onTap = { onDismissRequest() })
                },
        )
        AvatarPickerContent(
            avatars = avatars,
            selectedAvatarId = selectedAvatarId,
            onAvatarSelected = onAvatarSelected,
            onDismissRequest = onDismissRequest,
            layout = AvatarPickerLayout(
                gridMaxHeight = StreamCoreDimens.Tv.Profiles.AvatarPickerGridMaxHeight,
                itemSize = StreamCoreDimens.Tv.Profiles.AvatarPickerItemSize,
            ),
            gridState = gridState,
            closeControl = { onClick, enabled, controlModifier ->
                StreamCoreWebButton(
                    text = "Close",
                    onClick = onClick,
                    enabled = enabled,
                    variant = StreamCoreWebButtonVariant.Tertiary,
                    modifier = controlModifier.focusRequester(closeFocus),
                )
            },
            avatarControl = { _, index, _, onClick, controlModifier, content ->
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(index) {
                    if (index == selectedIndex && !initialFocusAssigned) {
                        focusRequester.requestFocus()
                        initialFocusAssigned = true
                    }
                }
                StreamCoreWebActionSurface(
                    onClick = onClick,
                    modifier = controlModifier.focusRequester(focusRequester),
                    content = content,
                )
            },
            modifier = Modifier.padding(StreamCoreDimens.Tv.Screen.HorizontalPadding)
                .widthIn(max = StreamCoreDimens.Tv.Profiles.AvatarPickerMaxWidth)
                .fillMaxWidth()
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        )
    }
}

private const val AvatarPickerScrimOpacity = 0.6f

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebAvatarPickerOverlayPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebAvatarPickerOverlay(
            avatars = ProfilesPreviewData.avatars,
            selectedAvatarId = ProfilesPreviewData.avatars.first().id,
            onAvatarSelected = {},
            onDismissRequest = {},
        )
    }
}
