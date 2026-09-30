package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.serialization.SerializationException
import kotlinx.io.IOException

/**
 * Converts Ktor, serialization, and IO failures into backend-agnostic [StreamCoreError] values.
 *
 * The mapper keeps HTTP status details in [StreamCoreErrorSource] so UI/domain layers can stay
 * independent of Ktor and TMDB response types.
 */
internal class TmdbErrorMapper constructor() {

    fun map(
        operation: String,
        throwable: Throwable,
    ): StreamCoreError {
        return when (throwable) {
            is TmdbAuthenticationFailureException -> StreamCoreError.Authentication(
                source = source(
                    operation = operation,
                    backendCode = throwable.backendCode,
                    backendMessage = throwable.message,
                ),
            )
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException -> StreamCoreError.Timeout(source = source(operation = operation))
            is ClientRequestException -> mapHttpError(
                operation = operation,
                exception = throwable,
            )
            is ServerResponseException -> mapHttpError(
                operation = operation,
                exception = throwable,
            )
            is RedirectResponseException -> mapHttpError(
                operation = operation,
                exception = throwable,
            )
            is ResponseException -> mapHttpError(
                operation = operation,
                exception = throwable,
            )
            is SerializationException -> StreamCoreError.Parsing(source = source(operation = operation))
            is IOException -> StreamCoreError.Network(source = source(operation = operation))
            else -> StreamCoreError.Unknown(source = source(operation = operation))
        }
    }

    private fun mapHttpError(
        operation: String,
        exception: ResponseException,
    ): StreamCoreError {
        val httpCode = exception.response.status.value
        val errorSource = source(
            operation = operation,
            httpCode = httpCode,
            backendMessage = exception.response.status.description,
        )

        return when {
            httpCode == 408 -> StreamCoreError.Timeout(source = errorSource)
            httpCode == 429 -> StreamCoreError.Server(source = errorSource)
            httpCode in 500..599 -> StreamCoreError.Server(source = errorSource)
            operation == LOGIN_OPERATION && (httpCode == 401 || httpCode == 403) -> {
                StreamCoreError.Authentication(source = errorSource)
            }
            httpCode == 401 || httpCode == 403 -> {
                StreamCoreError.Unauthorized(source = errorSource)
            }
            else -> StreamCoreError.Unknown(source = errorSource)
        }
    }

    private fun source(
        operation: String,
        httpCode: Int? = null,
        backendCode: String? = null,
        backendMessage: String? = null,
    ): StreamCoreErrorSource {
        return StreamCoreErrorSource(
            client = CLIENT,
            operation = operation,
            httpCode = httpCode,
            backendCode = backendCode,
            backendMessage = backendMessage,
        )
    }

    private companion object {
        const val CLIENT = "tmdb"
        const val LOGIN_OPERATION = "loginUser"
    }
}
