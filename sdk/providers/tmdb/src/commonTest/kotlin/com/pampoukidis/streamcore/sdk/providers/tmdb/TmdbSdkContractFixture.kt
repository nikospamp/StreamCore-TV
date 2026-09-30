package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbConnectionConfiguration
import com.pampoukidis.streamcoretv.client.tmdb.data.network.createTmdbHttpClient
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkPlatformStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

internal class TmdbSdkContractFixture {
    val requestedPaths = MutableStateFlow<List<String>>(emptyList())
    var failLogout: Boolean = false
    var rejectCredentials: Boolean = false
    var adultContent: Boolean = false

    fun create(demoPlayback: Boolean = true): StreamCoreClient {
        val connection = TmdbConnectionConfiguration("https://api.tmdb.test", "fixture-token")
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            requestedPaths.update { it + path }
            val status: HttpStatusCode
            val payload: String
            when {
                path == "/3/authentication/session" && request.method == HttpMethod.Delete && failLogout -> {
                    status = HttpStatusCode.ServiceUnavailable
                    payload = """{"status_code":9,"status_message":"Temporarily unavailable"}"""
                }
                path == "/3/authentication/token/validate_with_login" && rejectCredentials -> {
                    status = HttpStatusCode.Unauthorized
                    payload = """{"success":false,"status_code":30}"""
                }
                else -> {
                    status = HttpStatusCode.OK
                    payload = response(path)
                }
            }
            respond(payload, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        val http = createTmdbHttpClient(engine, connection, Json { ignoreUnknownKeys = true })
        return createTmdbSdk(
            TmdbSdkConfiguration(
                common = StreamCoreConfiguration("tmdb-reference", "provider-contract", persistence = StreamCorePersistenceMode.InMemory),
                connection = connection,
                demoPlayback = demoPlayback,
            ),
            SdkPlatformStorage.inMemory(),
            http,
        )
    }

    private fun response(path: String): String {
        return when {
            path == "/3/authentication/token/new" || path == "/3/authentication/token/validate_with_login" -> """{"success":true,"request_token":"fixture-request-token"}"""
            path == "/3/authentication/session/new" -> """{"success":true,"session_id":"fixture-session"}"""
            path == "/3/authentication/session" -> """{"success":true}"""
            path == "/3/account" -> """{"id":42,"username":"External","name":"External consumer"}"""
            path == "/3/movie/550/account_states" -> """{"id":550,"favorite":false,"watchlist":false}"""
            path == "/3/configuration" -> """{"images":{"secure_base_url":"https://images.test/","poster_sizes":["w500"],"backdrop_sizes":["w1280"],"profile_sizes":["w185"]}}"""
            path == "/3/genre/movie/list" -> """{"genres":[{"id":18,"name":"Drama"}]}"""
            path == "/3/movie/101" -> movie()
            path in listOf("/3/trending/movie/week", "/3/trending/movie/day", "/3/movie/popular", "/3/movie/now_playing", "/3/search/movie", "/3/movie/101/recommendations") -> """{"page":1,"total_pages":1,"total_results":1,"results":[${movie()}]}"""
            else -> error("Unexpected TMDB contract endpoint: $path")
        }
    }

    private fun movie(): String {
        return """{"id":101,"title":"Fixture Movie","overview":"Shared contract movie","adult":$adultContent,"poster_path":"/poster.jpg","backdrop_path":"/backdrop.jpg","release_date":"2024-01-01","vote_average":8.0,"genre_ids":[18],"genres":[{"id":18,"name":"Drama"}]}"""
    }
}
