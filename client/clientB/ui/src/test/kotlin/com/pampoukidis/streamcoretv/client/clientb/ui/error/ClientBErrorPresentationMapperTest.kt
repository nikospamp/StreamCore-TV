package com.pampoukidis.streamcoretv.client.clientb.ui.error

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientBErrorPresentationMapperTest {

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
    private val subject = ClientBErrorPresentationMapper(fallbackMapper)

    @Test
    fun `client B errors delegate to the common mapper`() {
        val authenticationResult = subject.map(AppError.Authentication())
        val networkResult = subject.map(AppError.Network())

        assertEquals(fallbackPresentation, authenticationResult)
        assertEquals(fallbackPresentation, networkResult)
    }
}
