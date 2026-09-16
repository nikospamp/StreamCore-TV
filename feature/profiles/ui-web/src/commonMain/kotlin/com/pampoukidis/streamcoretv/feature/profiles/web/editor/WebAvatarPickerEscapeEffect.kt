package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable

/** Browser keyboard ownership can be on the document after the underlying native input is removed.
 * This effect has no visual content; the overlay's screen preview covers its composition. */
@Composable
internal expect fun WebAvatarPickerEscapeEffect(onDismissRequest: () -> Unit)
