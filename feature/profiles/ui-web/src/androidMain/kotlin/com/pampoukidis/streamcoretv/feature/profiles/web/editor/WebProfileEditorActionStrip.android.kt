package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant

@Composable
internal actual fun WebProfileEditorActionStrip(
    isSaving: Boolean,
    canDelete: Boolean,
    focusRequest: WebProfileEditorActionTarget?,
    onFocusRequestConsumed: () -> Unit,
    onMoveUp: () -> Unit,
    onDisplayNameCommit: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    val cancel = remember { FocusRequester() }
    val save = remember { FocusRequester() }
    val delete = remember { FocusRequester() }
    LaunchedEffect(focusRequest) {
        when (focusRequest) {
            WebProfileEditorActionTarget.Cancel -> cancel.requestFocus()
            WebProfileEditorActionTarget.Save -> save.requestFocus()
            WebProfileEditorActionTarget.Delete -> delete.requestFocus()
            null -> return@LaunchedEffect
        }
        onFocusRequestConsumed()
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StreamCoreWebButton(
            text = "Cancel",
            onClick = onCancel,
            enabled = !isSaving,
            variant = StreamCoreWebButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(cancel)
                .focusProperties { right = save },
        )
        StreamCoreWebButton(
            text = "Save",
            onClick = onSave,
            loading = isSaving,
            modifier = Modifier
                .focusRequester(save)
                .focusProperties {
                    left = cancel
                    if (canDelete) right = delete
                },
        )
        if (canDelete) {
            StreamCoreWebButton(
                text = "Delete",
                onClick = onDelete,
                enabled = !isSaving,
                variant = StreamCoreWebButtonVariant.Destructive,
                modifier = Modifier
                    .focusRequester(delete)
                    .focusProperties { left = save },
            )
        }
    }
}
