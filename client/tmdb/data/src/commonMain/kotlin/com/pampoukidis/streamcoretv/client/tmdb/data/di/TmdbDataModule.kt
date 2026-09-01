package com.pampoukidis.streamcoretv.client.tmdb.data.di

import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbAuthStore
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbAuthenticateRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbPreferencesAuthStore
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbCatalogRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbDetailsRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
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
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

const val TMDB_AUTH_STORE_QUALIFIER = "tmdbAuthStore"
const val TMDB_JSON_QUALIFIER = "tmdbJson"

val tmdbDataModule = module {
    single<Json>(named(TMDB_JSON_QUALIFIER)) {
        Json { ignoreUnknownKeys = true }
    }
    single<TmdbApi> { KtorTmdbApi(httpClient = get()) }
    single { TmdbErrorMapper() }
    single { TmdbCallExecutor(errorMapper = get()) }
    single { TmdbReferenceDataSource(tmdbApi = get()) }
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
            accountId = get<TmdbRuntimeConfig>().accountId,
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
