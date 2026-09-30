package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile

@Composable
internal expect fun WebProfileEditorDeleteDialog(
    profile: StreamCoreProfile,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
)
