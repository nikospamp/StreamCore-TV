package com.pampoukidis.streamcoretv.data.client.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import javax.inject.Inject
import javax.inject.Named

class ErrorPresentationPresentationMapper @Inject constructor(
    @param:Named("defaultErrorPresentationMapper")
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorModel = when (error) {

        else -> defaultMapper.map(error)
    }
}