package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/** Native browser actions preserve keyboard activation and commit the current DOM input before save.
 * Preview rendering is supplied by the Android actual inside the screen previews. */
@Composable
internal expect fun WebProfileEditorActionButton(
    target: WebProfileEditorActionTarget,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onDisplayNameCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
)
