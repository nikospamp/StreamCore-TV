package com.pampoukidis.streamcoretv.data.tmdb.network

import com.pampoukidis.streamcoretv.data.tmdb.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Provides TMDB-specific networking dependencies.
 *
 * Ktor is installed only in `:data:tmdb`, so other provider modules can use a
 * different client stack without pulling this dependency through shared modules.
 */
@Module
@InstallIn(SingletonComponent::class)
object TmdbNetworkModule {

    @Provides
    @Named(TMDB_JSON)
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
        }
    }

    @Provides
    @Named(TMDB_BASE_URL)
    fun provideBaseUrl(): String {
        return BuildConfig.TMDB_BASE_URL
    }

    @Provides
    @Named(TMDB_READ_ACCESS_TOKEN)
    fun provideReadAccessToken(): String {
        return BuildConfig.TMDB_READ_ACCESS_TOKEN
    }

    /**
     * Builds the TMDB HTTP client with bearer-token auth and default response validation.
     */
    @Provides
    @Singleton
    fun provideHttpClient(
        @Named(TMDB_JSON) kotlinxJson: Json,
        @Named(TMDB_BASE_URL) baseUrl: String,
        @Named(TMDB_READ_ACCESS_TOKEN) readAccessToken: String,
    ): HttpClient {
        return HttpClient(OkHttp) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(kotlinxJson)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }

            defaultRequest {
                url(baseUrl)
                accept(ContentType.Application.Json)
                if (readAccessToken.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $readAccessToken")
                }
            }
        }
    }

    private const val TMDB_JSON = "tmdbJson"
    private const val TMDB_BASE_URL = "tmdbBaseUrl"
    private const val TMDB_READ_ACCESS_TOKEN = "tmdbReadAccessToken"
    private const val CONNECT_TIMEOUT_MILLIS = 10_000L
    private const val REQUEST_TIMEOUT_MILLIS = 15_000L
    private const val SOCKET_TIMEOUT_MILLIS = 15_000L
}
