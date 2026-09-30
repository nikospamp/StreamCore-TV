package com.pampoukidis.streamcore.sdk.providers.tmdb.ui.error

import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.tmdb_error_action_ok
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.tmdb_error_authentication_message
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.tmdb_error_authentication_title
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.error.ErrorUiModel

class TmdbErrorPresentationMapper constructor(
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: StreamCoreError): ErrorUiModel {
        return when (error) {
            is StreamCoreError.Authentication -> ErrorUiModel(
                title = Res.string.tmdb_error_authentication_title,
                message = Res.string.tmdb_error_authentication_message,
                confirmAction = Res.string.tmdb_error_action_ok,
            )

            else -> defaultMapper.map(error)
        }
    }
}
