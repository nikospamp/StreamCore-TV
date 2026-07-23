package com.pampoukidis.streamcoretv.client.clientb.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import javax.inject.Inject
import javax.inject.Named

class ClientBErrorPresentationMapper @Inject constructor(
    @param:Named("defaultErrorPresentationMapper")
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorModel {
        return defaultMapper.map(error)
    }
}
