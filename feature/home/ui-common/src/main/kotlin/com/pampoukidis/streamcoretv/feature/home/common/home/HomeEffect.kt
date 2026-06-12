package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface HomeEffect {
    data class ContentSelected(val contentId: String) : HomeEffect
    data class ShowError(val error: AppError) : HomeEffect
}