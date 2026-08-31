package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
internal fun TvProfileDeleteConfirmationDialog(
    profile: ProfileModel?,
    isSaving: Boolean,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (profile == null) {
        return
    }

    val cancelFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }

    LaunchedEffect(profile.id) {
        cancelFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = {
            Text(text = "Delete profile?")
        },
        text = {
            Text(text = "Delete ${profile.displayName}? This cannot be undone.")
        },
        confirmButton = {
            StreamCoreTvButton(
                text = "Delete",
                onClick = onConfirmDelete,
                enabled = !isSaving,
                loading = isSaving,
                variant = StreamCoreTvButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(deleteFocusRequester)
                    .focusProperties { left = cancelFocusRequester }
                    .testTag(ProfilesTestTags.ConfirmDeleteButton),
            )
        },
        dismissButton = {
            StreamCoreTvButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !isSaving,
                variant = StreamCoreTvButtonVariant.Tertiary,
                modifier = Modifier
                    .focusRequester(cancelFocusRequester)
                    .focusProperties { right = deleteFocusRequester }
                    .testTag(ProfilesTestTags.EditorCancelDeleteButton),
            )
        },
        modifier = Modifier.testTag(ProfilesTestTags.EditorDeleteConfirmation),
    )
}

@PreviewTV
@Composable
private fun TvProfileDeleteConfirmationDialogPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileDeleteConfirmationDialog(
            profile = ProfilesPreviewData.profiles.first { it.canDelete },
            isSaving = false,
            onConfirmDelete = {},
            onDismiss = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvProfileDeleteConfirmationDialogSavingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileDeleteConfirmationDialog(
            profile = ProfilesPreviewData.profiles.first { it.canDelete },
            isSaving = true,
            onConfirmDelete = {},
            onDismiss = {},
        )
    }
}
