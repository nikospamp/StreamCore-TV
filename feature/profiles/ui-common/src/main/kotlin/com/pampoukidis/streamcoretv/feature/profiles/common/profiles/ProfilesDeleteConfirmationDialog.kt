package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

@Composable
fun ProfilesDeleteConfirmationDialog(
    profile: ProfileModel?,
    isSaving: Boolean,
    onAction: (ProfilesAction) -> Unit,
) {
    if (profile == null) return

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onAction(ProfilesAction.DismissDeleteConfirmation)
        },
        title = {
            Text(text = "Delete profile")
        },
        text = {
            Text(text = "Delete ${profile.displayName}? This cannot be undone.")
        },
        confirmButton = {
            StreamCoreButton(
                text = "Delete",
                onClick = { onAction(ProfilesAction.ConfirmDeleteProfile) },
                enabled = !isSaving,
                loading = isSaving,
            )
        },
        dismissButton = {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = { onAction(ProfilesAction.DismissDeleteConfirmation) },
                enabled = !isSaving,
            )
        },
    )
}

@PreviewMobile
@Composable
private fun ProfilesDeleteConfirmationDialogPreview() {
    StreamCoreTVTheme {
        ProfilesDeleteConfirmationDialog(
            profile = ProfilesPreviewData.profiles.first(),
            isSaving = false,
            onAction = {},
        )
    }
}
