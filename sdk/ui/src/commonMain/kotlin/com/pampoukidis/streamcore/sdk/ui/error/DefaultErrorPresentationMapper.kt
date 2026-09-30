package com.pampoukidis.streamcore.sdk.ui.error

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.ui.generated.resources.profile_pin_title
import com.pampoukidis.streamcore.sdk.ui.generated.resources.profile_pin_incorrect
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_action_ok
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_authentication_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_authentication_title
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_generic_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_generic_title
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_network_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_network_title
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_session_expired_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_session_expired_title
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_timeout_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_timeout_title
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_unauthorized_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_unauthorized_title

class DefaultErrorPresentationMapper constructor() : ErrorPresentationMapper {

    override fun map(error: StreamCoreError): ErrorUiModel {
        return when (error) {
            is StreamCoreError.PinRejected -> ErrorUiModel(
                title = Res.string.profile_pin_title,
                message = Res.string.profile_pin_incorrect,
                confirmAction = Res.string.error_action_ok,
            )
            is StreamCoreError.Authentication -> ErrorUiModel(
                title = Res.string.error_authentication_title,
                message = Res.string.error_authentication_message,
                confirmAction = Res.string.error_action_ok,
            )

            is StreamCoreError.Network -> ErrorUiModel(
                title = Res.string.error_network_title,
                message = Res.string.error_network_message,
                confirmAction = Res.string.error_action_ok,
            )

            is StreamCoreError.Timeout -> ErrorUiModel(
                title = Res.string.error_timeout_title,
                message = Res.string.error_timeout_message,
                confirmAction = Res.string.error_action_ok,
            )

            is StreamCoreError.Unauthorized -> ErrorUiModel(
                title = Res.string.error_unauthorized_title,
                message = Res.string.error_unauthorized_message,
                confirmAction = Res.string.error_action_ok,
            )

            is StreamCoreError.SessionExpired -> ErrorUiModel(
                title = Res.string.error_session_expired_title,
                message = Res.string.error_session_expired_message,
                confirmAction = Res.string.error_action_ok,
                dismissible = false,
            )

            is StreamCoreError.Server -> ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            )

            is StreamCoreError.Parsing -> ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            )

            is StreamCoreError.Unsupported,
            is StreamCoreError.Validation,
            is StreamCoreError.InvalidContext,
            is StreamCoreError.Storage,
            is StreamCoreError.Closed,
            is StreamCoreError.Unknown -> ErrorUiModel(
                title = Res.string.error_generic_title,
                message = Res.string.error_generic_message,
                confirmAction = Res.string.error_action_ok,
            )
        }
    }
}
