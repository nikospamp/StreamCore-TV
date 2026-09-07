package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/** Focus/key handling hooks. Custom controls must apply the supplied modifier. */
@Immutable
data class ProfileEditorModifiers(
    val close: Modifier = Modifier,
    val save: Modifier = Modifier,
    val avatar: Modifier = Modifier,
    val displayName: Modifier = Modifier,
    val kids: Modifier = Modifier,
    val delete: Modifier = Modifier,
)
