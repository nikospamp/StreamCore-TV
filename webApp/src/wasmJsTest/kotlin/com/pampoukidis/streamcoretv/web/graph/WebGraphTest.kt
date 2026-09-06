package com.pampoukidis.streamcoretv.web.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsViewModel
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryViewModel
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginViewModel
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerViewModel
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorViewModel
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchViewModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.resolveViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
class WebGraphTest {
    private val viewModelStore = ViewModelStore()
    private var application: KoinApplication? = null

    @BeforeTest
    fun setUp() {
        // Keep constructor-launched work queued: graph checks must not call the backend.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        try {
            viewModelStore.clear()
        } finally {
            try {
                application?.close()
            } finally {
                Dispatchers.resetMain()
            }
        }
    }

    @Test
    fun startupResolutionDoesNotConstructScreenViewModelsOrPlaybackSessions() {
        val forbiddenStartupWork = module {
            viewModel<LoginViewModel> { error("Startup constructed LoginViewModel") }
            viewModel<ProfilesViewModel> { error("Startup constructed ProfilesViewModel") }
            viewModel<ProfileEditorViewModel> { error("Startup constructed ProfileEditorViewModel") }
            viewModel<HomeViewModel> { error("Startup constructed HomeViewModel") }
            viewModel<SearchViewModel> { error("Startup constructed SearchViewModel") }
            viewModel<DetailsViewModel> { error("Startup constructed DetailsViewModel") }
            viewModel<LibraryViewModel> { error("Startup constructed LibraryViewModel") }
            viewModel<PlayerViewModel> { error("Startup constructed PlayerViewModel") }
            single<PlaybackSessionFactory> {
                object : PlaybackSessionFactory {
                    override fun create(): PlaybackSession {
                        error("Startup created a playback session")
                    }
                }
            }
        }
        val graph = createGraph(forbiddenStartupWork)

        val resolved = resolveWebGraph(graph.koin)

        assertEquals(
            listOf(
                "AuthenticateRepository",
                "ProfileRepository",
                "HomeRepository",
                "SearchRepository",
                "RecentSearchRepository",
                "DetailsRepository",
                "LibraryRepository",
                "PlaybackProgressRepository",
                "PlaybackSourceRepository",
                "PlaybackSessionFactory",
            ),
            resolved,
        )
    }

    @Test
    fun tmdbOnlyGraphResolvesScreenFactoriesIntoOwnedViewModelStore() {
        val graph = createGraph()

        assertEquals(1, graph.koin.getAll<PlaybackSessionFactory>().size)
        assertOwnedFactory<LoginViewModel>(graph)
        assertOwnedFactory<ProfilesViewModel>(graph)
        assertOwnedFactory<ProfileEditorViewModel>(graph)
        assertOwnedFactory<HomeViewModel>(graph)
        assertOwnedFactory<SearchViewModel>(graph)
        assertOwnedFactory<DetailsViewModel>(graph)
        assertOwnedFactory<LibraryViewModel>(graph)
    }

    private fun createGraph(vararg overrides: Module): KoinApplication {
        val graph = koinApplication {
            modules(
                webModules(
                    config = WebRuntimeConfig(
                        tmdbBaseUrl = "https://api.example.test/3/",
                        tmdbReadAccessToken = "browser-visible-token",
                        tmdbAccountId = "42",
                    ),
                    useSessionStorage = true,
                ),
            )
            modules(overrides.toList())
        }
        application = graph
        return graph
    }

    private inline fun <reified T : ViewModel> assertOwnedFactory(graph: KoinApplication) {
        val viewModel = resolveViewModel<T>(
            vmClass = T::class,
            viewModelStore = viewModelStore,
            extras = CreationExtras.Empty,
            scope = graph.koin.scopeRegistry.rootScope,
        )
        assertSame(
            viewModel,
            resolveViewModel<T>(
                vmClass = T::class,
                viewModelStore = viewModelStore,
                extras = CreationExtras.Empty,
                scope = graph.koin.scopeRegistry.rootScope,
            ),
        )
    }
}
