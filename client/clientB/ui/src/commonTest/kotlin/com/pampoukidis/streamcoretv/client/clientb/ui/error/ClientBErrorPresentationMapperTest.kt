package com.pampoukidis.streamcoretv.client.clientb.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.error.ErrorUiModel
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.error_action_ok
import streamcoretv.core.ui.generated.resources.error_generic_message
import streamcoretv.core.ui.generated.resources.error_generic_title
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientBErrorPresentationMapperTest {

    private val fallbackPresentation = ErrorUiModel(
        title = Res.string.error_generic_title,
        message = Res.string.error_generic_message,
        confirmAction = Res.string.error_action_ok,
    )
    private val fallbackMapper = object : ErrorPresentationMapper {
        override fun map(error: AppError): ErrorUiModel {
            return fallbackPresentation
        }
    }
    private val subject = ClientBErrorPresentationMapper(fallbackMapper)

    @Test
    fun `client B errors delegate to the common mapper`() {
        val authenticationResult = subject.map(AppError.Authentication())
        val networkResult = subject.map(AppError.Network())

        assertEquals(fallbackPresentation, authenticationResult)
        assertEquals(fallbackPresentation, networkResult)
    }
}
