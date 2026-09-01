package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pampoukidis.streamcoretv.core.ui.error.ErrorUiModel
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.error_action_ok
import streamcoretv.core.ui.generated.resources.error_generic_message
import streamcoretv.core.ui.generated.resources.error_generic_title
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ErrorHost(
    presentation: ErrorUiModel?,
    onDismiss: () -> Unit,
) {
    presentation ?: return

    AlertDialog(
        onDismissRequest = {
            if (presentation.dismissible) {
                onDismiss()
            }
        },
        title = {
            Text(text = stringResource(presentation.title))
        },
        text = {
            Text(text = stringResource(presentation.message))
        },
        confirmButton = {
            StreamCoreTextButton(
                text = stringResource(presentation.confirmAction),
                onClick = onDismiss,
                enabled = true,
            )
        },
    )
}

@Preview
@Composable
private fun ErrorHostPreview() {
    StreamCoreTheme {
        ErrorHost(
            presentation = ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            ),
            onDismiss = {},
        )
    }
}
