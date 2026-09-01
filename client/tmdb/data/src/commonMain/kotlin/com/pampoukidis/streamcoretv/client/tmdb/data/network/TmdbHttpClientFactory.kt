package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun createTmdbHttpClient(
    engine: HttpClientEngine,
    config: TmdbRuntimeConfig,
    json: Json,
): HttpClient {
    return HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        }
        defaultRequest {
            url(config.baseUrl)
            accept(ContentType.Application.Json)
            if (config.readAccessToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${config.readAccessToken}")
            }
        }
    }
}

internal const val CONNECT_TIMEOUT_MILLIS = 10_000L
internal const val REQUEST_TIMEOUT_MILLIS = 15_000L
internal const val SOCKET_TIMEOUT_MILLIS = 15_000L
