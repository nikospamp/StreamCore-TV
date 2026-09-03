package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun WebProfileEditorActionStrip(
    isSaving: Boolean,
    canDelete: Boolean,
    focusRequest: WebProfileEditorActionTarget?,
    onFocusRequestConsumed: () -> Unit,
    onMoveUp: () -> Unit,
    onDisplayNameCommit: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
)
