package com.pampoukidis.streamcoretv.core.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.error_action_ok
import streamcoretv.core.ui.generated.resources.error_authentication_message
import streamcoretv.core.ui.generated.resources.error_authentication_title
import streamcoretv.core.ui.generated.resources.error_generic_message
import streamcoretv.core.ui.generated.resources.error_generic_title
import streamcoretv.core.ui.generated.resources.error_network_message
import streamcoretv.core.ui.generated.resources.error_network_title
import streamcoretv.core.ui.generated.resources.error_session_expired_message
import streamcoretv.core.ui.generated.resources.error_session_expired_title
import streamcoretv.core.ui.generated.resources.error_timeout_message
import streamcoretv.core.ui.generated.resources.error_timeout_title
import streamcoretv.core.ui.generated.resources.error_unauthorized_message
import streamcoretv.core.ui.generated.resources.error_unauthorized_title

class DefaultErrorPresentationPresentationMapper constructor() : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorUiModel {
        return when (error) {
            is AppError.Authentication -> ErrorUiModel(
                title = Res.string.error_authentication_title,
                message = Res.string.error_authentication_message,
                confirmAction = Res.string.error_action_ok,
            )

            is AppError.Network -> ErrorUiModel(
                title = Res.string.error_network_title,
                message = Res.string.error_network_message,
                confirmAction = Res.string.error_action_ok,
            )

            is AppError.Timeout -> ErrorUiModel(
                title = Res.string.error_timeout_title,
                message = Res.string.error_timeout_message,
                confirmAction = Res.string.error_action_ok,
            )

            is AppError.Unauthorized -> ErrorUiModel(
                title = Res.string.error_unauthorized_title,
                message = Res.string.error_unauthorized_message,
                confirmAction = Res.string.error_action_ok,
            )

            is AppError.SessionExpired -> ErrorUiModel(
                title = Res.string.error_session_expired_title,
                message = Res.string.error_session_expired_message,
                confirmAction = Res.string.error_action_ok,
                dismissible = false,
            )

            is AppError.Server -> ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            )

            is AppError.Parsing -> ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            )

            is AppError.Unknown -> ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            )
        }
    }
}
