package com.pampoukidis.streamcoretv.core.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError

fun interface ErrorPresentationMapper {
    fun map(error: AppError): ErrorUiModel
}
