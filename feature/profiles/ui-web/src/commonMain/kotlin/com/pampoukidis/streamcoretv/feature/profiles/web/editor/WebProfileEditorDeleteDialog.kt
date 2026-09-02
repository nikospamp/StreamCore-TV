package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel

@Composable
internal expect fun WebProfileEditorDeleteDialog(
    profile: ProfileModel,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
)
