package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
internal actual fun WebProfileEditorDeleteDialog(
    profile: ProfileModel,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = StreamCoreDimens.Elevation.Medium,
            modifier = Modifier
                .webEscape { if (!isSaving) onDismiss() }
                .testTag(ProfilesTestTags.EditorDeleteConfirmation)
                .semantics { contentDescription = "Delete ${profile.displayName} profile confirmation" },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.padding(StreamCoreWebDimens.PanelPadding),
            ) {
                Text("Delete ${profile.displayName}?", style = MaterialTheme.typography.headlineMedium)
                Text("This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    StreamCoreWebButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        enabled = !isSaving,
                        variant = StreamCoreWebButtonVariant.Secondary,
                        modifier = Modifier
                            .focusRequester(cancelFocus)
                            .focusProperties {
                                left = confirmFocus
                                right = confirmFocus
                                up = confirmFocus
                                down = confirmFocus
                            }
                            .testTag(ProfilesTestTags.EditorCancelDeleteButton),
                    )
                    StreamCoreWebButton(
                        text = "Delete",
                        onClick = onConfirm,
                        loading = isSaving,
                        variant = StreamCoreWebButtonVariant.Destructive,
                        modifier = Modifier
                            .focusRequester(confirmFocus)
                            .focusProperties {
                                left = cancelFocus
                                right = cancelFocus
                                up = cancelFocus
                                down = cancelFocus
                            }
                            .testTag(ProfilesTestTags.ConfirmDeleteButton),
                    )
                }
            }
        }
    }
}
