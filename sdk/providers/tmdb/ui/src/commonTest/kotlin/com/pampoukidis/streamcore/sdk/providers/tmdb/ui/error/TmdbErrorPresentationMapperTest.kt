package com.pampoukidis.streamcore.sdk.providers.tmdb.ui.error

import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.tmdb_error_action_ok
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.tmdb_error_authentication_message
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources.tmdb_error_authentication_title
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.error.ErrorUiModel
import com.pampoukidis.streamcore.sdk.ui.generated.resources.Res as SdkRes
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_action_ok
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_generic_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_generic_title
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbErrorPresentationMapperTest {

    private val fallbackPresentation = ErrorUiModel(
        title = SdkRes.string.error_generic_title,
        message = SdkRes.string.error_generic_message,
        confirmAction = SdkRes.string.error_action_ok,
    )
    private val fallbackMapper = object : ErrorPresentationMapper {
        override fun map(error: StreamCoreError): ErrorUiModel {
            return fallbackPresentation
        }
    }
    private val subject = TmdbErrorPresentationMapper(fallbackMapper)

    @Test
    fun `authentication error uses TMDB presentation resources`() {
        val result = subject.map(StreamCoreError.Authentication())

        assertEquals(Res.string.tmdb_error_authentication_title, result.title)
        assertEquals(Res.string.tmdb_error_authentication_message, result.message)
        assertEquals(Res.string.tmdb_error_action_ok, result.confirmAction)
    }

    @Test
    fun `non TMDB-specific error delegates to the common mapper`() {
        val result = subject.map(StreamCoreError.Network())

        assertEquals(fallbackPresentation, result)
    }
}
