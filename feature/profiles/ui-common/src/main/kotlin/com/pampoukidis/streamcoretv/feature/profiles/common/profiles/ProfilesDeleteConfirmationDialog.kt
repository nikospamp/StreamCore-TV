package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun ProfilesDeleteConfirmationDialog(
    profile: ProfileModel?,
    isSaving: Boolean,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (profile == null) return

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
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
                onClick = onConfirmDelete,
                enabled = !isSaving,
                loading = isSaving,
                modifier = Modifier.testTag(ProfilesTestTags.ConfirmDeleteButton),
            )
        },
        dismissButton = {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !isSaving,
            )
        },
    )
}

@PreviewMobile
@Composable
private fun ProfilesDeleteConfirmationDialogPreview() {
    StreamCoreTheme {
        ProfilesDeleteConfirmationDialog(
            profile = ProfilesPreviewData.profiles.first(),
            isSaving = false,
            onConfirmDelete = {},
            onDismiss = {},
        )
    }
}
