package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun WebProfileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
)
