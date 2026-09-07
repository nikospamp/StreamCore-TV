package com.pampoukidis.streamcoretv.feature.profiles.mobile.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerContent
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

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
        AvatarPickerContent(
            avatars = avatars,
            selectedAvatarId = selectedAvatarId,
            onAvatarSelected = onAvatarSelected,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.fillMaxWidth(0.9f)
                .widthIn(max = StreamCoreDimens.Mobile.Profiles.AvatarPickerMaxWidth),
        )
    }
}

@PreviewMobile
@Composable
private fun MobileAvatarPickerDialogPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AvatarPickerContent(
                    avatars = ProfilesPreviewData.avatars,
                    selectedAvatarId = ProfilesPreviewData.avatars.first().id,
                    onAvatarSelected = {},
                    onDismissRequest = {},
                    modifier = Modifier.fillMaxWidth(0.9f)
                        .widthIn(max = StreamCoreDimens.Mobile.Profiles.AvatarPickerMaxWidth),
                )
            }
        }
    }
}
