package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface AppAuthEffect {
    data class ShowError(
        val error: AppError,
    ) : AppAuthEffect
}