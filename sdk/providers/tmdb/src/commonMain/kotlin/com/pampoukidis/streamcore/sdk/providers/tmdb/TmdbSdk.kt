package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.providers.tmdb.auth.TmdbAuthenticateRepository
import com.pampoukidis.streamcore.sdk.providers.tmdb.auth.TmdbPreferencesAuthStore
import com.pampoukidis.streamcore.sdk.providers.tmdb.catalog.TmdbReferenceDataSource
import com.pampoukidis.streamcore.sdk.providers.tmdb.details.TmdbDetailsRepository
import com.pampoukidis.streamcore.sdk.providers.tmdb.home.TmdbHomeRepository
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.*
import com.pampoukidis.streamcore.sdk.providers.tmdb.playback.TmdbPlaybackSourceRepository
import com.pampoukidis.streamcore.sdk.providers.tmdb.profile.TmdbProfileRepository
import com.pampoukidis.streamcore.sdk.providers.tmdb.search.TmdbSearchRepository
import com.pampoukidis.streamcore.sdk.runtime.RuntimeStreamCoreClient
import com.pampoukidis.streamcore.sdk.runtime.content.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.error.ProviderOperationException
import com.pampoukidis.streamcore.sdk.runtime.session.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.session.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkPlatformStorage
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/** Local reference subprofiles and optional, explicitly labelled sample playback. */
object TmdbSdk

internal fun createTmdbSdk(
    config: TmdbSdkConfiguration,
    storage: SdkPlatformStorage,
    httpClient: HttpClient
): StreamCoreClient {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val api = KtorTmdbApi(
        httpClient = httpClient,
        configuredLocale = config.common.locale,
        configuredRegion = config.common.region
    )
    val calls = TmdbCallExecutor(TmdbErrorMapper())
    val reference = TmdbReferenceDataSource(api)
    return RuntimeStreamCoreClient(
        configuration = config.common,
        capabilities = StreamCoreCapabilities(playback = if (config.demoPlayback) StreamCorePlaybackSupport.DemoMedia else StreamCorePlaybackSupport.Unsupported),
        authentication = TmdbAuthenticateRepository(
            tmdbApi = api,
            callExecutor = calls,
            authStore = TmdbPreferencesAuthStore(storage.auth),
            accountId = config.common.expectedAccountId.orEmpty()
        ),
        sessions = ProviderSessionFactory { account ->
            val profiles = TmdbProfileRepository(storage.auth, json, account.id)
            val home = TmdbHomeRepository(api, reference, calls, profiles)
            val details = TmdbDetailsRepository(api, reference, calls, profiles)
            val search = TmdbSearchRepository(api, reference, calls, profiles)
            val contentPolicy = object : ContentPolicyProvider {
                override suspend fun isContentAllowed(
                    profile: StreamCoreProfile,
                    content: StreamCoreContent
                ): Boolean {
                    if (!profile.isKidsProfile) return true
                    val movieId = content.id.toIntOrNull() ?: return false
                    val result =
                        calls.execute("contentPolicy") { !api.getMovieDetails(movieId).adult }
                    return when (result) {
                        is StreamCoreResult.Success -> result.value
                        is StreamCoreResult.Failure -> throw ProviderOperationException(result.error)
                    }
                }
            }
            ProviderSessionServices(
                profiles = profiles,
                home = home,
                details = details,
                search = search,
                playback = TmdbPlaybackSourceRepository(),
                contentPolicy = contentPolicy,
            )
        },
        local = PreferencesSdkStorage.create(
            libraryStore = storage.library,
            searchStore = storage.search,
            progressStore = storage.progress,
            json = json,
            authStore = storage.auth
        ),
        closeResources = { httpClient.close(); storage.close() },
    )
}
