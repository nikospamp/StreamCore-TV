package com.pampoukidis.streamcoretv.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.window.Dialog
import com.pampoukidis.streamcoretv.core.model.general.Platform
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButtonVariant
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform

@Composable
fun AppLogoutConfirmationDialog(
    visible: Boolean,
    isLogoutInProgress: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    platform: Platform = rememberLoginPlatform(),
) {
    if (!visible) {
        return
    }

    if (platform == Platform.Tv) {
        TvLogoutConfirmationDialog(
            isLogoutInProgress = isLogoutInProgress,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        return
    }

    TouchLogoutConfirmationDialog(
        isLogoutInProgress = isLogoutInProgress,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun TouchLogoutConfirmationDialog(
    isLogoutInProgress: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLogoutInProgress) {
                onDismiss()
            }
        },
        title = { Text(text = "Sign out?") },
        text = { Text(text = "You’ll need to sign in again to watch with this account.") },
        confirmButton = {
            StreamCoreButton(
                text = "Sign out",
                onClick = onConfirm,
                enabled = !isLogoutInProgress,
                loading = isLogoutInProgress,
                variant = StreamCoreButtonVariant.Secondary,
                modifier = Modifier
                    .logoutProgressSemantics(isLogoutInProgress)
                    .testTag(AppAuthTestTags.ConfirmLogoutButton),
            )
        },
        dismissButton = {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !isLogoutInProgress,
                modifier = Modifier.testTag(AppAuthTestTags.CancelLogoutButton),
            )
        },
        modifier = Modifier.testTag(AppAuthTestTags.LogoutConfirmation),
    )
}

@Composable
private fun TvLogoutConfirmationDialog(
    isLogoutInProgress: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = {
            if (!isLogoutInProgress) {
                onDismiss()
            }
        },
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = StreamCoreDimens.Elevation.Medium,
            modifier = Modifier
                .widthIn(max = StreamCoreDimens.Tv.Panel.Width)
                .testTag(AppAuthTestTags.LogoutConfirmation),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier.padding(StreamCoreDimens.Tv.Panel.Padding),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
                    Text(
                        text = "Sign out?",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "You’ll need to sign in again to watch with this account.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StreamCoreTvButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        enabled = !isLogoutInProgress,
                        variant = StreamCoreTvButtonVariant.Tertiary,
                        modifier = Modifier
                            .focusRequester(cancelFocusRequester)
                            .focusProperties { right = confirmFocusRequester }
                            .testTag(AppAuthTestTags.CancelLogoutButton),
                    )
                    StreamCoreTvButton(
                        text = "Sign out",
                        onClick = onConfirm,
                        enabled = !isLogoutInProgress,
                        loading = isLogoutInProgress,
                        variant = StreamCoreTvButtonVariant.Secondary,
                        modifier = Modifier
                            .focusRequester(confirmFocusRequester)
                            .focusProperties { left = cancelFocusRequester }
                            .logoutProgressSemantics(isLogoutInProgress)
                            .testTag(AppAuthTestTags.ConfirmLogoutButton),
                    )
                }
            }
        }
    }
}

private fun Modifier.logoutProgressSemantics(isLogoutInProgress: Boolean): Modifier {
    return semantics(mergeDescendants = true) {
        contentDescription = "Sign out"
        stateDescription = if (isLogoutInProgress) "Signing out" else "Ready"
    }
}

@PreviewMobile
@PreviewTablet
@PreviewTV
@Composable
private fun AppLogoutConfirmationDialogPreview() {
    StreamCoreTheme {
        AppLogoutConfirmationDialog(
            visible = true,
            isLogoutInProgress = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
