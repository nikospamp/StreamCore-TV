package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
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
 * Converts Ktor, serialization, and IO failures into backend-agnostic [AppError] values.
 *
 * The mapper keeps HTTP status details in [ErrorSource] so UI/domain layers can stay
 * independent of Ktor and TMDB response types.
 */
internal class TmdbErrorMapper constructor() {

    fun map(
        operation: String,
        throwable: Throwable,
    ): AppError {
        return when (throwable) {
            is TmdbAuthenticationFailureException -> AppError.Authentication(
                source = source(
                    operation = operation,
                    backendCode = throwable.backendCode,
                    backendMessage = throwable.message,
                ),
            )
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException -> AppError.Timeout(source = source(operation = operation))
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
            is SerializationException -> AppError.Parsing(source = source(operation = operation))
            is IOException -> AppError.Network(source = source(operation = operation))
            else -> AppError.Unknown(source = source(operation = operation))
        }
    }

    private fun mapHttpError(
        operation: String,
        exception: ResponseException,
    ): AppError {
        val httpCode = exception.response.status.value
        val errorSource = source(
            operation = operation,
            httpCode = httpCode,
            backendMessage = exception.response.status.description,
        )

        return when {
            operation == LOGIN_OPERATION && httpCode in 400..499 -> {
                AppError.Authentication(source = errorSource)
            }
            httpCode == 401 || httpCode == 403 -> {
                AppError.Unauthorized(source = errorSource)
            }
            httpCode == 408 -> AppError.Timeout(source = errorSource)
            httpCode == 429 -> AppError.Server(source = errorSource)
            httpCode in 500..599 -> AppError.Server(source = errorSource)
            else -> AppError.Unknown(source = errorSource)
        }
    }

    private fun source(
        operation: String,
        httpCode: Int? = null,
        backendCode: String? = null,
        backendMessage: String? = null,
    ): ErrorSource {
        return ErrorSource(
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
