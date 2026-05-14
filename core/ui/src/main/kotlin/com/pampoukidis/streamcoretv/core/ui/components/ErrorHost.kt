package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet

@Composable
fun ErrorHost(
    presentation: ErrorModel?,
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
            Text(text = stringResource(presentation.titleRes))
        },
        text = {
            Text(text = stringResource(presentation.messageRes))
        },
        confirmButton = {
            StreamCoreTextButton(
                text = stringResource(presentation.confirmActionRes),
                onClick = onDismiss,
                enabled = true,
            )
        },
    )
}

@PreviewMobile
@PreviewTablet
@PreviewTV
@Composable
private fun ErrorHostPreview() {
    StreamCoreTVTheme {
        ErrorHost(
            presentation = ErrorModel(
                titleRes = R.string.error_generic_title,
                messageRes = R.string.error_generic_message,
                confirmActionRes = R.string.error_action_ok,
            ),
            onDismiss = {},
        )
    }
}
