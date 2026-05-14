package com.pampoukidis.streamcoretv.data.client.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.data.client.R
import javax.inject.Inject
import javax.inject.Named

class ErrorPresentationPresentationMapper @Inject constructor(
    @param:Named("defaultErrorPresentationMapper")
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorModel = when (error) {
        is AppError.Authentication -> ErrorModel(
            titleRes = R.string.client_a_error_authentication_title,
            messageRes = R.string.client_a_error_authentication_message,
            confirmActionRes = R.string.error_action_ok,
        )

        else -> defaultMapper.map(error)
    }
}