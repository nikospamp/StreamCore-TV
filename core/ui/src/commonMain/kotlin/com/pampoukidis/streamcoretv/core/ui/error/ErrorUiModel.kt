package com.pampoukidis.streamcoretv.core.ui.error

import org.jetbrains.compose.resources.StringResource

data class ErrorUiModel(
    val title: StringResource,
    val message: StringResource,
    val confirmAction: StringResource,
    val dismissible: Boolean = true,
)
