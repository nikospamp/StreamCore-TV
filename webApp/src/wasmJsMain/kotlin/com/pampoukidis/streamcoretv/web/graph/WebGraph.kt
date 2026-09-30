package com.pampoukidis.streamcoretv.web.graph

import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.avatar.TmdbProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.error.TmdbErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.api.AuthService
import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.api.HomeService
import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcoretv.core.tracing.NoOpPerformanceTracer
import com.pampoukidis.streamcoretv.core.tracing.PerformanceTracer
import com.pampoukidis.streamcoretv.core.ui.error.coreUiModule
import com.pampoukidis.streamcore.sdk.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.error.DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.feature.details.common.details.detailsUiModule
import com.pampoukidis.streamcoretv.feature.home.common.home.homeUiModule
import com.pampoukidis.streamcoretv.feature.library.common.library.libraryUiModule
import com.pampoukidis.streamcoretv.feature.login.common.login.loginUiModule
import com.pampoukidis.streamcoretv.feature.player.common.player.playerUiModule
import com.pampoukidis.streamcoretv.feature.profiles.common.profilesUiModule
import com.pampoukidis.streamcoretv.feature.search.common.search.searchUiModule
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.web.webPlaybackModule
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfig
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdk
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdkConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.createWeb
import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcoretv.web.storage.WebSdkStorageException

suspend fun startWebGraph(
    config: WebRuntimeConfig,
    useSessionStorage: Boolean,
): WebGraphHandle {
    val application = koinApplication {
        modules(webModules(config, useSessionStorage))
    }
    return try {
        val resolved = resolveWebGraph(application.koin)
        // Exercise storage through the public SDK before choosing persistent versus session mode.
        val initialBootstrapResult = application.koin.get<StreamCoreClient>().bootstrap()
        if (initialBootstrapResult is StreamCoreResult.Failure && initialBootstrapResult.error is StreamCoreError.Storage) {
            throw WebSdkStorageException()
        }
        WebGraphHandle(
            application = application,
            resolvedDefinitions = resolved,
            storageNames = listOf("StreamCore SDK: streamcore"),
            initialBootstrapResult = initialBootstrapResult,
        )
    } catch (throwable: Throwable) {
        application.close()
        throw throwable
    }
}

fun webModules(
    config: WebRuntimeConfig,
    useSessionStorage: Boolean,
): List<Module> {
    val webRuntimeModule = module {
        single<StreamCoreClient> {
            TmdbSdk.createWeb(
                config = TmdbSdkConfiguration(
                    common = StreamCoreConfiguration(
                        backend = "tmdb-production",
                        storageNamespace = "streamcore",
                        expectedAccountId = config.tmdbAccountId.takeIf { it.isNotBlank() },
                    ),
                    connection = config.toTmdbRuntimeConfig(),
                    demoPlayback = true,
                    legacyApplicationStorage = true,
                ),
                useSessionStorage = useSessionStorage,
            )
        } onClose { client -> client?.close() }
        single<AuthService> { get<StreamCoreClient>().auth }
        single<ProfileService> { get<StreamCoreClient>().profiles }
        single<HomeService> { get<StreamCoreClient>().home }
        single<DetailsService> { get<StreamCoreClient>().details }
        single<SearchService> { get<StreamCoreClient>().search }
        single<LibraryService> { get<StreamCoreClient>().library }
        single<PlaybackService> { get<StreamCoreClient>().playback }
        single<PerformanceTracer> { NoOpPerformanceTracer }
        single<ProfileAvatarArtworkResolver> { TmdbProfileAvatarArtworkResolver() }
        single<ErrorPresentationMapper> {
            TmdbErrorPresentationMapper(get(named(DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER)))
        }
    }
    return listOf(
        webRuntimeModule,
        coreUiModule,
        loginUiModule,
        profilesUiModule,
        homeUiModule,
        searchUiModule,
        detailsUiModule,
        libraryUiModule,
        webPlaybackModule,
        playerUiModule,
    )
}

data class WebGraphHandle(
    val application: KoinApplication,
    val resolvedDefinitions: List<String>,
    val storageNames: List<String>,
    private var initialBootstrapResult: StreamCoreResult<StreamCoreContext>? = null,
) : AutoCloseable {
    internal fun takeInitialBootstrapResult(): StreamCoreResult<StreamCoreContext>? {
        val result = initialBootstrapResult
        initialBootstrapResult = null
        return result
    }

    override fun close() {
        application.close()
    }
}

internal fun resolveWebGraph(koin: Koin): List<String> {
    return buildList {
        resolve<StreamCoreClient>(koin, "StreamCoreClient")
        resolve<AuthService>(koin, "AuthService")
        resolve<ProfileService>(koin, "ProfileService")
        resolve<HomeService>(koin, "HomeService")
        resolve<SearchService>(koin, "SearchService")
        resolve<DetailsService>(koin, "DetailsService")
        resolve<LibraryService>(koin, "LibraryService")
        resolve<PlaybackService>(koin, "PlaybackService")
        resolve<PlaybackSessionFactory>(koin, "PlaybackSessionFactory")
    }
}

private inline fun <reified T : Any> MutableList<String>.resolve(koin: Koin, name: String) {
    koin.get<T>()
    add(name)
}
