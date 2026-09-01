package com.pampoukidis.streamcoretv.client.tmdb.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.client.tmdb.data.BuildConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbAuthStore
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbAuthenticateRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbPreferencesAuthStore
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbCatalogRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbDetailsRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.network.KtorTmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbErrorMapper
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.search.TmdbSearchRepository
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
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
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

const val TMDB_ACCOUNT_ID_QUALIFIER = "tmdbAccountId"
const val TMDB_AUTH_STORE_QUALIFIER = "tmdbAuthStore"
const val TMDB_BASE_URL_QUALIFIER = "tmdbBaseUrl"
const val TMDB_JSON_QUALIFIER = "tmdbJson"
const val TMDB_READ_ACCESS_TOKEN_QUALIFIER = "tmdbReadAccessToken"

val tmdbDataModule = module {
    single<Json>(named(TMDB_JSON_QUALIFIER)) {
        Json { ignoreUnknownKeys = true }
    }
    single(named(TMDB_BASE_URL_QUALIFIER)) { BuildConfig.TMDB_BASE_URL }
    single(named(TMDB_READ_ACCESS_TOKEN_QUALIFIER)) { BuildConfig.TMDB_READ_ACCESS_TOKEN }
    single(named(TMDB_ACCOUNT_ID_QUALIFIER)) { BuildConfig.TMDB_ACCOUNT_ID }
    single<HttpClient> {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(get(named(TMDB_JSON_QUALIFIER)))
            }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
            defaultRequest {
                url(get<String>(named(TMDB_BASE_URL_QUALIFIER)))
                accept(ContentType.Application.Json)
                val readAccessToken = get<String>(named(TMDB_READ_ACCESS_TOKEN_QUALIFIER))
                if (readAccessToken.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $readAccessToken")
                }
            }
        }
    }
    single<TmdbApi> { KtorTmdbApi(httpClient = get()) }
    single { TmdbErrorMapper() }
    single { TmdbCallExecutor(errorMapper = get()) }
    single { TmdbReferenceDataSource(tmdbApi = get()) }
    single<DataStore<Preferences>>(named(TMDB_AUTH_STORE_QUALIFIER)) {
        PreferenceDataStoreFactory.create(
            produceFile = {
                androidContext().preferencesDataStoreFile(TMDB_AUTH_STORE_FILE)
            },
        )
    }
    single<TmdbAuthStore> {
        TmdbPreferencesAuthStore(
            dataStore = get(named(TMDB_AUTH_STORE_QUALIFIER)),
        )
    }
    single<AuthenticateRepository> {
        TmdbAuthenticateRepository(
            tmdbApi = get(),
            callExecutor = get(),
            authStore = get(),
            accountId = get(named(TMDB_ACCOUNT_ID_QUALIFIER)),
        )
    }
    single {
        TmdbCatalogRepository(
            tmdbApi = get(),
            referenceDataSource = get(),
            callExecutor = get(),
        )
    } bind HomeRepository::class
    single {
        TmdbDetailsRepository(
            tmdbApi = get(),
            referenceDataSource = get(),
            callExecutor = get(),
        )
    } bind DetailsRepository::class
    single { TmdbProfileRepository() } bind ProfileRepository::class
    single {
        TmdbSearchRepository(
            tmdbApi = get(),
            referenceDataSource = get(),
            callExecutor = get(),
        )
    } bind SearchRepository::class
}

private const val TMDB_AUTH_STORE_FILE = "tmdb_auth.preferences_pb"
private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val SOCKET_TIMEOUT_MILLIS = 15_000L
