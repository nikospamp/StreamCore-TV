package com.pampoukidis.streamcore.sdk.providers.clientb.ui.error

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.error.ErrorUiModel
import com.pampoukidis.streamcore.sdk.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_action_ok
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_generic_message
import com.pampoukidis.streamcore.sdk.ui.generated.resources.error_generic_title
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientBErrorPresentationMapperTest {

    private val fallbackPresentation = ErrorUiModel(
        title = Res.string.error_generic_title,
        message = Res.string.error_generic_message,
        confirmAction = Res.string.error_action_ok,
    )
    private val fallbackMapper = object : ErrorPresentationMapper {
        override fun map(error: StreamCoreError): ErrorUiModel {
            return fallbackPresentation
        }
    }
    private val subject = ClientBErrorPresentationMapper(fallbackMapper)

    @Test
    fun `client B errors delegate to the common mapper`() {
        val authenticationResult = subject.map(StreamCoreError.Authentication())
        val networkResult = subject.map(StreamCoreError.Network())

        assertEquals(fallbackPresentation, authenticationResult)
        assertEquals(fallbackPresentation, networkResult)
    }
}
