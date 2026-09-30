package com.pampoukidis.streamcore.sdk.providers.clientb.ui.error

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.error.ErrorUiModel

class ClientBErrorPresentationMapper constructor(
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: StreamCoreError): ErrorUiModel {
        return defaultMapper.map(error)
    }
}
