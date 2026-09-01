package com.pampoukidis.streamcoretv.client.tmdb.ui.error

import streamcoretv.client.tmdb.ui.generated.resources.Res
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_error_action_ok
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_error_authentication_message
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_error_authentication_title
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.error.ErrorUiModel

class TmdbErrorPresentationMapper constructor(
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorUiModel {
        return when (error) {
            is AppError.Authentication -> ErrorUiModel(
                title = Res.string.tmdb_error_authentication_title,
                message = Res.string.tmdb_error_authentication_message,
                confirmAction = Res.string.tmdb_error_action_ok,
            )

            else -> defaultMapper.map(error)
        }
    }
}
