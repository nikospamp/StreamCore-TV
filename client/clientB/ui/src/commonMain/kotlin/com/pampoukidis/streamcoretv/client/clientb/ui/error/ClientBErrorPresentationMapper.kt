package com.pampoukidis.streamcoretv.client.clientb.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.error.ErrorUiModel

class ClientBErrorPresentationMapper constructor(
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorUiModel {
        return defaultMapper.map(error)
    }
}
