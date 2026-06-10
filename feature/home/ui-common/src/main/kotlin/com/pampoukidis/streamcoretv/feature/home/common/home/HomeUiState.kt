package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.content.RowModel

data class HomeUiState(
    val isLoading: Boolean = true,
    val rows: List<RowModel> = emptyList(),
)