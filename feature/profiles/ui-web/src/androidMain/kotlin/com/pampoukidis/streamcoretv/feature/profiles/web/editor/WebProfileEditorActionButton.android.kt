package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant

@Composable
internal actual fun WebProfileEditorActionButton(
    target: WebProfileEditorActionTarget,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onDisplayNameCommit: (String) -> Unit,
    modifier: Modifier,
    requestFocus: Boolean,
) {
    StreamCoreWebButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        variant = if (target == WebProfileEditorActionTarget.Delete) {
            StreamCoreWebButtonVariant.Destructive
        } else {
            StreamCoreWebButtonVariant.Tertiary
        },
        modifier = modifier,
    )
}
