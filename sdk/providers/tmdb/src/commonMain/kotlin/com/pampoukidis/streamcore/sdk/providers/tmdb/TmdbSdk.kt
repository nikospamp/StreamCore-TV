package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbAuthenticateRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbPreferencesAuthStore
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbCatalogRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbDetailsRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.network.*
import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.search.TmdbSearchRepository
import com.pampoukidis.streamcoretv.client.tmdb.player.TmdbPlaybackSourceRepository
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderOperationException
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.runtime.RuntimeStreamCoreClient
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkPlatformStorage
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/** Local reference subprofiles and optional, explicitly labelled sample playback. */
object TmdbSdk

internal fun createTmdbSdk(config: TmdbSdkConfiguration, storage: SdkPlatformStorage, httpClient: HttpClient): StreamCoreClient {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val api = KtorTmdbApi(httpClient, configuredLocale = config.common.locale, configuredRegion = config.common.region)
    val calls = TmdbCallExecutor(TmdbErrorMapper())
    val reference = TmdbReferenceDataSource(api)
    return RuntimeStreamCoreClient(
        configuration = config.common,
        capabilities = StreamCoreCapabilities(playback = if (config.demoPlayback) StreamCorePlaybackSupport.DemoMedia else StreamCorePlaybackSupport.Unsupported),
        authentication = TmdbAuthenticateRepository(api, calls, TmdbPreferencesAuthStore(storage.auth), accountId = config.common.expectedAccountId.orEmpty()),
        sessions = ProviderSessionFactory { account ->
            val profiles = TmdbProfileRepository(storage.auth, json, account.id)
            val home = TmdbCatalogRepository(api, reference, calls, profiles)
            val details = TmdbDetailsRepository(api, reference, calls, profiles)
            val search = TmdbSearchRepository(api, reference, calls, profiles)
            val contentPolicy = object : ContentPolicyProvider {
                override suspend fun isContentAllowed(profile: StreamCoreProfile, content: StreamCoreContent): Boolean {
                    if (!profile.isKidsProfile) return true
                    val movieId = content.id.toIntOrNull() ?: return false
                    return when (val result = calls.execute("contentPolicy") { !api.getMovieDetails(movieId).adult }) {
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
        local = PreferencesSdkStorage.create(storage.library, storage.search, storage.progress, json, authStore = storage.auth),
        closeResources = { httpClient.close(); storage.close() },
    )
}
