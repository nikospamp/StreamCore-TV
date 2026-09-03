@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebBlockingSurface
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerViewModel
import com.pampoukidis.streamcoretv.feature.player.common.player.playerUiModule
import com.pampoukidis.streamcoretv.feature.player.web.player.WebPlayerRoute
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import com.pampoukidis.streamcoretv.web.navigation.isBrowserSafeId
import com.pampoukidis.streamcoretv.web.playback.DiagnosticPlaybackSessionFactory
import com.pampoukidis.streamcoretv.web.playback.DiagnosticPlaybackSourceRepository
import com.pampoukidis.streamcoretv.web.playback.DiagnosticPlayerRegistry
import com.pampoukidis.streamcoretv.web.playback.DiagnosticPlayerScenario
import com.pampoukidis.streamcoretv.web.startup.WebStartupState
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.w3c.dom.events.Event

@Composable
internal fun WebDiagnosticPlayerDestination(
    state: WebStartupState.Ready,
    destination: WebRoute.DiagnosticPlayer,
) {
    remember {
        DiagnosticListenerProbe.install()
        true
    }
    val query = currentDiagnosticPlayerQuery()
    val requestedId = query.requestedId ?: destination.contentId
    val scenario = query.fixture?.let(DiagnosticPlayerScenario::fromQueryValue)
        ?: DiagnosticPlayerRegistry.pendingScenario
    val profileId = query.profileId ?: DiagnosticPlayerRegistry.pendingProfileId

    if (!requestedId.isBrowserSafeId()) {
        InvalidDiagnosticPlayerDestination(
            route = destination,
            fixtureName = query.fixture ?: InvalidFixtureName,
        )
        return
    }

    val fixtureFactory = remember(destination.contentId, requestedId, scenario, profileId) {
        DiagnosticPlayerRegistry.beginFixture(scenario, profileId)
        DiagnosticPlaybackSessionFactory(scenario)
    }
    val progressRepository = remember(state.graph) {
        state.graph.application.koin.get<PlaybackProgressRepository>()
    }
    val diagnosticApplication = remember(fixtureFactory, progressRepository) {
        koinApplication {
            modules(
                module {
                    single<PlaybackSourceRepository> { DiagnosticPlaybackSourceRepository }
                    single<PlaybackProgressRepository> { progressRepository }
                    single<PlaybackSessionFactory> { fixtureFactory }
                },
                playerUiModule,
            )
        }
    }
    val storeOwner = remember(destination.contentId, requestedId, scenario, profileId) {
        DiagnosticPlayerViewModelStoreOwner()
    }
    val request = remember(requestedId, profileId) {
        diagnosticPlaybackRequest(profileId, requestedId)
    }

    DisposableEffect(diagnosticApplication, storeOwner) {
        onDispose {
            document.body?.removeAttribute(FixtureReadyAttribute)
            storeOwner.viewModelStore.clear()
            diagnosticApplication.close()
        }
    }

    KoinIsolatedContext(diagnosticApplication) {
        val viewModel = koinViewModel<PlayerViewModel>(
            key = "diagnostic-player:$profileId:$requestedId",
            viewModelStoreOwner = storeOwner,
        )
        DiagnosticPlayerEvidence(
            route = destination,
            scenario = scenario,
            profileId = profileId,
            viewModel = viewModel,
        )
        WebPlayerRoute(
            request = request,
            onBack = { window.history.back() },
            viewModel = viewModel,
        )
    }
}

@Composable
private fun InvalidDiagnosticPlayerDestination(
    route: WebRoute.DiagnosticPlayer,
    fixtureName: String,
) {
    DisposableEffect(route, fixtureName) {
        val body = document.body
        body?.removeAttribute(FixtureReadyAttribute)
        body?.setAttribute("data-product-route", route.path)
        body?.setAttribute("data-player-fixture", fixtureName)
        body?.setAttribute("data-player-invalid-request", "true")
        body?.setAttribute("data-player-active-sessions", DiagnosticPlayerRegistry.activeSessions.toString())
        body?.setAttribute("data-player-active-listeners", DiagnosticListenerProbe.count().toString())
        body?.setAttribute("data-player-active-timers", DiagnosticPlayerRegistry.activeTimers.toString())
        body?.setAttribute("data-player-close-count", DiagnosticPlayerRegistry.closeCount.toString())
        body?.setAttribute("data-player-prepare-count", DiagnosticPlayerRegistry.prepareCount.toString())
        body?.setAttribute(FixtureReadyAttribute, "true")
        onDispose {
            body?.removeAttribute(FixtureReadyAttribute)
        }
    }
    StreamCoreWebBlockingSurface(
        title = "Invalid playback request",
        message = "Return to a title and choose Play again.",
    )
}

@Composable
private fun DiagnosticPlayerEvidence(
    route: WebRoute.DiagnosticPlayer,
    scenario: DiagnosticPlayerScenario,
    profileId: String,
    viewModel: PlayerViewModel,
) {
    val playerState by viewModel.uiState.collectAsState()
    var fullscreen by remember { mutableStateOf(document.fullscreenElement != null) }

    DisposableEffect(Unit) {
        val fullscreenListener: (Event) -> Unit = {
            fullscreen = document.fullscreenElement != null
        }
        document.addEventListener(FullscreenEvent, fullscreenListener)
        onDispose {
            document.removeEventListener(FullscreenEvent, fullscreenListener)
        }
    }

    LaunchedEffect(
        route,
        scenario,
        profileId,
        playerState,
        fullscreen,
        DiagnosticPlayerRegistry.activeSessions,
        DiagnosticListenerProbe.count(),
        DiagnosticPlayerRegistry.activeTimers,
        DiagnosticPlayerRegistry.closeCount,
        DiagnosticPlayerRegistry.prepareCount,
        DiagnosticPlayerRegistry.activationRequired,
    ) {
        publishDiagnosticPlayerEvidence(
            route = route,
            scenario = scenario,
            profileId = profileId,
            state = playerState,
            fullscreen = fullscreen,
        )
    }
}

private fun publishDiagnosticPlayerEvidence(
    route: WebRoute.DiagnosticPlayer,
    scenario: DiagnosticPlayerScenario,
    profileId: String,
    state: PlayerUiState,
    fullscreen: Boolean,
) {
    val body = document.body ?: return
    body.removeAttribute(FixtureReadyAttribute)
    body.setAttribute("data-product-route", route.path)
    body.setAttribute("data-player-fixture", scenario.queryValue)
    body.setAttribute("data-player-invalid-request", "false")
    body.setAttribute("data-player-profile-id", profileId)
    body.setAttribute("data-player-phase", state.phase.name.lowercase())
    body.setAttribute("data-player-playing", state.isPlaying.toString())
    body.setAttribute(
        "data-player-activation-required",
        DiagnosticPlayerRegistry.activationRequired.toString(),
    )
    body.setAttribute("data-player-error-code", state.error?.code.orEmpty())
    body.setAttribute("data-player-error-message", state.error?.message.orEmpty())
    body.setAttribute("data-player-prepare-count", DiagnosticPlayerRegistry.prepareCount.toString())
    body.setAttribute("data-player-position-ms", state.positionMillis.toString())
    body.setAttribute("data-player-speed", state.speed.toString())
    body.setAttribute("data-player-video-track", state.selectedVideoTrackId.orEmpty())
    body.setAttribute("data-player-audio-track", state.selectedAudioTrackId.orEmpty())
    body.setAttribute("data-player-text-track", state.selectedTextTrackId.orEmpty())
    body.setAttribute("data-player-fullscreen", fullscreen.toString())
    body.setAttribute("data-player-layer", state.diagnosticLayer())
    body.setAttribute(
        "data-player-filmstrip-count",
        state.filmstripFrames.count { frame -> frame.image != null }.toString(),
    )
    body.setAttribute("data-player-canvas-capture-count", "0")
    body.setAttribute("data-player-active-sessions", DiagnosticPlayerRegistry.activeSessions.toString())
    body.setAttribute("data-player-active-listeners", DiagnosticListenerProbe.count().toString())
    body.setAttribute("data-player-active-timers", DiagnosticPlayerRegistry.activeTimers.toString())
    body.setAttribute("data-player-close-count", DiagnosticPlayerRegistry.closeCount.toString())
    if (state.isExpectedFixtureState(scenario)) {
        body.setAttribute(FixtureReadyAttribute, "true")
    }
}

@Composable
internal fun DiagnosticRouteEvidence(route: WebRoute) {
    DisposableEffect(
        route,
        DiagnosticPlayerRegistry.activeSessions,
        DiagnosticListenerProbe.count(),
        DiagnosticPlayerRegistry.activeTimers,
        DiagnosticPlayerRegistry.closeCount,
    ) {
        val body = document.body
        body?.setAttribute("data-product-route", route.path)
        body?.setAttribute("data-player-active-sessions", DiagnosticPlayerRegistry.activeSessions.toString())
        body?.setAttribute("data-player-active-listeners", DiagnosticListenerProbe.count().toString())
        body?.setAttribute("data-player-active-timers", DiagnosticPlayerRegistry.activeTimers.toString())
        body?.setAttribute("data-player-close-count", DiagnosticPlayerRegistry.closeCount.toString())
        onDispose { }
    }
}

internal fun prepareDiagnosticPlayerLaunchFromCurrentLocation() {
    val query = currentDiagnosticPlayerQuery()
    DiagnosticPlayerRegistry.prepareSameDocumentLaunch(
        scenario = DiagnosticPlayerScenario.fromQueryValue(query.fixture),
        profileId = query.profileId ?: DiagnosticPlayerRegistry.DefaultProfileId,
    )
}

private fun PlayerUiState.isExpectedFixtureState(scenario: DiagnosticPlayerScenario): Boolean {
    return if (scenario == DiagnosticPlayerScenario.RecoverableError) {
        phase == PlaybackPhase.Error || phase == PlaybackPhase.Ready
    } else {
        phase == PlaybackPhase.Ready
    }
}

private fun PlayerUiState.diagnosticLayer(): String {
    if (error != null) {
        return "error"
    }
    return when (settingsPage) {
        null -> "player"
        PlayerSettingsPage.Root -> "settings-root"
        else -> "settings-detail"
    }
}

private fun diagnosticPlaybackRequest(
    profileId: String,
    contentId: String,
): PlaybackRequestModel {
    return PlaybackRequestModel(
        profileId = profileId,
        contentId = contentId,
        contentSnapshot = ContentModel(
            id = contentId,
            title = "Diagnostic feature film",
            description = "Synthetic backend-free playback fixture.",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        ),
    )
}

private fun currentDiagnosticPlayerQuery(): DiagnosticPlayerQuery {
    val parameters = window.location.search
        .removePrefix("?")
        .split('&')
        .mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                entry.substring(0, separator) to entry.substring(separator + 1)
            }
        }
        .toMap()
    return DiagnosticPlayerQuery(
        fixture = parameters["fixture"],
        profileId = parameters["profile"],
        requestedId = parameters["requestedId"],
    )
}

private data class DiagnosticPlayerQuery(
    val fixture: String?,
    val profileId: String?,
    val requestedId: String?,
)

private class DiagnosticPlayerViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

private const val FixtureReadyAttribute = "data-player-fixture-ready"
private const val InvalidFixtureName = "invalid-direct-id"
private const val FullscreenEvent = "fullscreenchange"

@JsModule("./diagnostic-listener-probe.mjs")
private external object DiagnosticListenerProbe {
    fun install()
    fun count(): Int
}
