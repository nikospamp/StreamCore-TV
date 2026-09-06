package com.pampoukidis.streamcoretv.web.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class WebRuntimeConfigLoader(
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val loadText: suspend () -> String = ::fetchRuntimeConfigText,
) {
    suspend fun load(): WebRuntimeConfigLoadResult {
        return try {
            val config = json.decodeFromString<WebRuntimeConfig>(loadText())
            when (val validation = config.validate()) {
                is WebRuntimeConfigValidationResult.Valid -> {
                    WebRuntimeConfigLoadResult.Success(validation.config)
                }
                is WebRuntimeConfigValidationResult.Invalid -> {
                    WebRuntimeConfigLoadResult.Failure(validation.guidance)
                }
            }
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (_: SerializationException) {
            WebRuntimeConfigLoadResult.Failure(
                "/config.json is not valid for the documented WebRuntimeConfig schema.",
            )
        } catch (_: ClientRequestException) {
            WebRuntimeConfigLoadResult.Failure(
                "/config.json was not found. For local development, configure TMDB in local.properties " +
                        "and run :webApp:wasmJsBrowserDevelopmentRun. For deployment, serve /config.json " +
                        "using the config.example.json schema.",
            )
        } catch (_: ServerResponseException) {
            WebRuntimeConfigLoadResult.Failure(
                "/config.json could not be loaded because the server returned an error.",
            )
        } catch (throwable: Throwable) {
            WebRuntimeConfigLoadResult.Failure(
                "Unable to load /config.json: ${throwable.message ?: "unknown browser network error"}.",
            )
        }
    }
}

sealed interface WebRuntimeConfigLoadResult {
    data class Success(val config: WebRuntimeConfig) : WebRuntimeConfigLoadResult
    data class Failure(val guidance: String) : WebRuntimeConfigLoadResult
}

private suspend fun fetchRuntimeConfigText(): String {
    val client = HttpClient(Js) {
        expectSuccess = true
    }
    return try {
        client.get("/config.json").bodyAsText()
    } finally {
        client.close()
    }
}
