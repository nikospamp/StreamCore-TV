package com.pampoukidis.streamcoretv.client.tmdb.ui.error

import com.pampoukidis.streamcoretv.client.tmdb.ui.R
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import javax.inject.Inject
import javax.inject.Named

class TmdbErrorPresentationMapper @Inject constructor(
    @param:Named("defaultErrorPresentationMapper")
    private val defaultMapper: ErrorPresentationMapper,
) : ErrorPresentationMapper {

    override fun map(error: AppError): ErrorModel {
        return when (error) {
            is AppError.Authentication -> ErrorModel(
                titleRes = R.string.tmdb_error_authentication_title,
                messageRes = R.string.tmdb_error_authentication_message,
                confirmActionRes = R.string.tmdb_error_action_ok,
            )

            else -> defaultMapper.map(error)
        }
    }
}
