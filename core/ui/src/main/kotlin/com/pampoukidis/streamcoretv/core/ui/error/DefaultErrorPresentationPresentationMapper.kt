package com.pampoukidis.streamcoretv.core.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.ui.R
import javax.inject.Inject

class DefaultErrorPresentationPresentationMapper @Inject constructor() : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorModel = when (error) {
        is AppError.Authentication -> ErrorModel(
            titleRes = R.string.error_authentication_title,
            messageRes = R.string.error_authentication_message,
            confirmActionRes = R.string.error_action_ok,
        )

        is AppError.Network -> ErrorModel(
            titleRes = R.string.error_network_title,
            messageRes = R.string.error_network_message,
            confirmActionRes = R.string.error_action_ok,
        )

        is AppError.Timeout -> ErrorModel(
            titleRes = R.string.error_timeout_title,
            messageRes = R.string.error_timeout_message,
            confirmActionRes = R.string.error_action_ok,
        )

        is AppError.Unauthorized -> ErrorModel(
            titleRes = R.string.error_unauthorized_title,
            messageRes = R.string.error_unauthorized_message,
            confirmActionRes = R.string.error_action_ok,
        )

        is AppError.SessionExpired -> ErrorModel(
            titleRes = R.string.error_session_expired_title,
            messageRes = R.string.error_session_expired_message,
            confirmActionRes = R.string.error_action_ok,
            dismissible = false,
        )

        is AppError.Server -> ErrorModel(
            titleRes = R.string.error_generic_title,
            messageRes = R.string.error_generic_message,
            confirmActionRes = R.string.error_action_ok,
        )

        is AppError.Parsing -> ErrorModel(
            titleRes = R.string.error_generic_title,
            messageRes = R.string.error_generic_message,
            confirmActionRes = R.string.error_action_ok,
        )

        is AppError.Unknown -> ErrorModel(
            titleRes = R.string.error_generic_title,
            messageRes = R.string.error_generic_message,
            confirmActionRes = R.string.error_action_ok,
        )
    }
}