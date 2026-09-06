package com.pampoukidis.streamcoretv.web.graph

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.di.tmdbDataModule
import com.pampoukidis.streamcoretv.client.tmdb.data.di.tmdbWebDataModule
import com.pampoukidis.streamcoretv.client.tmdb.data.di.TMDB_AUTH_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.client.tmdb.player.tmdbPlayerModule
import com.pampoukidis.streamcoretv.client.tmdb.ui.avatar.tmdbProfileAvatarArtworkModule
import com.pampoukidis.streamcoretv.client.tmdb.ui.error.tmdbErrorPresentationModule
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.tracing.NoOpPerformanceTracer
import com.pampoukidis.streamcoretv.core.tracing.PerformanceTracer
import com.pampoukidis.streamcoretv.core.ui.error.coreUiModule
import com.pampoukidis.streamcoretv.feature.details.common.details.detailsUiModule
import com.pampoukidis.streamcoretv.feature.details.domain.detailsDomainModule
import com.pampoukidis.streamcoretv.feature.home.common.home.homeUiModule
import com.pampoukidis.streamcoretv.feature.home.domain.homeDomainModule
import com.pampoukidis.streamcoretv.feature.library.common.library.libraryUiModule
import com.pampoukidis.streamcoretv.feature.library.data.libraryWebDataModule
import com.pampoukidis.streamcoretv.feature.library.data.LIBRARY_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.feature.library.domain.libraryDomainModule
import com.pampoukidis.streamcoretv.feature.login.common.login.loginUiModule
import com.pampoukidis.streamcoretv.feature.login.domain.loginDomainModule
import com.pampoukidis.streamcoretv.feature.player.common.player.playerUiModule
import com.pampoukidis.streamcoretv.feature.player.data.playbackWebDataModule
import com.pampoukidis.streamcoretv.feature.player.data.PLAYBACK_PROGRESS_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.feature.profiles.common.profilesUiModule
import com.pampoukidis.streamcoretv.feature.profiles.domain.profilesDomainModule
import com.pampoukidis.streamcoretv.feature.search.common.search.searchUiModule
import com.pampoukidis.streamcoretv.feature.search.data.searchWebDataModule
import com.pampoukidis.streamcoretv.feature.search.data.SEARCH_HISTORY_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.feature.search.domain.RecentSearchRepository
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import com.pampoukidis.streamcoretv.feature.search.domain.searchDomainModule
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.playback.web.webPlaybackModule
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfig
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlinx.coroutines.flow.first

suspend fun startWebGraph(
    config: WebRuntimeConfig,
    useSessionStorage: Boolean,
): WebGraphHandle {
    val application = koinApplication {
        modules(webModules(config, useSessionStorage))
    }
    return try {
        val resolved = resolveWebGraph(application.koin)
        val storageNames = probeWebDataStores(application.koin)
        WebGraphHandle(
            application = application,
            resolvedDefinitions = resolved,
            storageNames = storageNames,
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
        single<TmdbRuntimeConfig> { config.toTmdbRuntimeConfig() }
        single<PerformanceTracer> { NoOpPerformanceTracer }
    }
    return listOf(
        webRuntimeModule,
        coreUiModule,
        loginDomainModule,
        loginUiModule,
        profilesDomainModule,
        profilesUiModule,
        homeDomainModule,
        homeUiModule,
        searchWebDataModule(useSessionStorage),
        searchDomainModule,
        searchUiModule,
        detailsDomainModule,
        detailsUiModule,
        libraryWebDataModule(useSessionStorage),
        libraryDomainModule,
        libraryUiModule,
        playbackWebDataModule(useSessionStorage),
        webPlaybackModule,
        playerUiModule,
        tmdbDataModule,
        tmdbWebDataModule(useSessionStorage),
        tmdbPlayerModule,
        tmdbProfileAvatarArtworkModule,
        tmdbErrorPresentationModule,
    )
}

data class WebGraphHandle(
    val application: KoinApplication,
    val resolvedDefinitions: List<String>,
    val storageNames: List<String>,
) : AutoCloseable {
    override fun close() {
        application.close()
    }
}

private suspend fun probeWebDataStores(koin: Koin): List<String> {
    WebStoreSpecs.forEach { spec ->
        val store = koin.get<DataStore<Preferences>>(named(spec.qualifier))
        store.edit { preferences ->
            preferences[WebRuntimeProbeKey] = true
        }
        check(store.data.first()[WebRuntimeProbeKey] == true)
    }
    return WebStoreSpecs.map(WebStoreSpec::name)
}

internal fun resolveWebGraph(koin: Koin): List<String> {
    return buildList {
        resolve<AuthenticateRepository>(koin, "AuthenticateRepository")
        resolve<ProfileRepository>(koin, "ProfileRepository")
        resolve<HomeRepository>(koin, "HomeRepository")
        resolve<SearchRepository>(koin, "SearchRepository")
        resolve<RecentSearchRepository>(koin, "RecentSearchRepository")
        resolve<DetailsRepository>(koin, "DetailsRepository")
        resolve<LibraryRepository>(koin, "LibraryRepository")
        resolve<PlaybackProgressRepository>(koin, "PlaybackProgressRepository")
        resolve<PlaybackSourceRepository>(koin, "PlaybackSourceRepository")
        resolve<PlaybackSessionFactory>(koin, "PlaybackSessionFactory")
    }
}

private inline fun <reified T : Any> MutableList<String>.resolve(koin: Koin, name: String) {
    koin.get<T>()
    add(name)
}

private data class WebStoreSpec(
    val name: String,
    val qualifier: String,
)

private val WebStoreSpecs = listOf(
    WebStoreSpec(
        name = "tmdb_auth.preferences_pb",
        qualifier = TMDB_AUTH_STORE_QUALIFIER,
    ),
    WebStoreSpec(
        name = "search_history.preferences_pb",
        qualifier = SEARCH_HISTORY_STORE_QUALIFIER,
    ),
    WebStoreSpec(
        name = "library.preferences_pb",
        qualifier = LIBRARY_STORE_QUALIFIER,
    ),
    WebStoreSpec(
        name = "playback_progress.preferences_pb",
        qualifier = PLAYBACK_PROGRESS_STORE_QUALIFIER,
    ),
)
private val WebRuntimeProbeKey = booleanPreferencesKey("web_runtime_probe")
