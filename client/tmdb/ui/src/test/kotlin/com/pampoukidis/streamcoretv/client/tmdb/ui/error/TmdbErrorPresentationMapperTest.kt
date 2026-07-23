package com.pampoukidis.streamcoretv.client.tmdb.ui.error

import com.pampoukidis.streamcoretv.client.tmdb.ui.R
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbErrorPresentationMapperTest {

    private val fallbackPresentation = ErrorModel(
        titleRes = 1,
        messageRes = 2,
        confirmActionRes = 3,
    )
    private val fallbackMapper = object : ErrorPresentationMapper {
        override fun map(error: AppError): ErrorModel {
            return fallbackPresentation
        }
    }
    private val subject = TmdbErrorPresentationMapper(fallbackMapper)

    @Test
    fun `authentication error uses TMDB presentation resources`() {
        val result = subject.map(AppError.Authentication())

        assertEquals(R.string.tmdb_error_authentication_title, result.titleRes)
        assertEquals(R.string.tmdb_error_authentication_message, result.messageRes)
        assertEquals(R.string.tmdb_error_action_ok, result.confirmActionRes)
    }

    @Test
    fun `non TMDB-specific error delegates to the common mapper`() {
        val result = subject.map(AppError.Network())

        assertEquals(fallbackPresentation, result)
    }
}
