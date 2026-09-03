package com.pampoukidis.streamcoretv.feature.search.web.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun WebSearchTextField(
    value: String,
    enabled: Boolean,
    requestFocus: Boolean,
    onValueChange: (String) -> Unit,
    onSubmitCommittedValue: (String) -> Unit,
    onEscape: () -> Unit,
    onFocusRequestConsumed: () -> Unit,
    modifier: Modifier = Modifier,
)
