package com.pampoukidis.streamcore.sdk.ui.error

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

fun interface ErrorPresentationMapper {
    fun map(error: StreamCoreError): ErrorUiModel
}
