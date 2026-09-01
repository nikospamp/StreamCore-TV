package com.pampoukidis.streamcoretv.client.tmdb.ui.error

import streamcoretv.client.tmdb.ui.generated.resources.Res
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_error_action_ok
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_error_authentication_message
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_error_authentication_title
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.error.ErrorUiModel
import streamcoretv.core.ui.generated.resources.Res as CoreRes
import streamcoretv.core.ui.generated.resources.error_action_ok
import streamcoretv.core.ui.generated.resources.error_generic_message
import streamcoretv.core.ui.generated.resources.error_generic_title
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbErrorPresentationMapperTest {

    private val fallbackPresentation = ErrorUiModel(
        title = CoreRes.string.error_generic_title,
        message = CoreRes.string.error_generic_message,
        confirmAction = CoreRes.string.error_action_ok,
    )
    private val fallbackMapper = object : ErrorPresentationMapper {
        override fun map(error: AppError): ErrorUiModel {
            return fallbackPresentation
        }
    }
    private val subject = TmdbErrorPresentationMapper(fallbackMapper)

    @Test
    fun `authentication error uses TMDB presentation resources`() {
        val result = subject.map(AppError.Authentication())

        assertEquals(Res.string.tmdb_error_authentication_title, result.title)
        assertEquals(Res.string.tmdb_error_authentication_message, result.message)
        assertEquals(Res.string.tmdb_error_action_ok, result.confirmAction)
    }

    @Test
    fun `non TMDB-specific error delegates to the common mapper`() {
        val result = subject.map(AppError.Network())

        assertEquals(fallbackPresentation, result)
    }
}
