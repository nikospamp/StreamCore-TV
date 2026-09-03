package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButtonVariant
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import com.pampoukidis.streamcoretv.web.platform.SecureWebUriHandler
import com.pampoukidis.streamcoretv.web.startup.WebStartupState

object WebDiagnosticTags {
    const val Loading = "web-startup-loading"
    const val BlockingError = "web-startup-blocking-error"
    const val Ready = "web-startup-ready"
    const val StorageWarning = "web-storage-warning"
    const val Route = "web-current-route"
    const val Graph = "web-graph-smoke"
    const val DetailsNavigation = "web-navigate-details"
    const val PlayerNavigation = "web-navigate-player"
    const val RootNavigation = "web-navigate-root"
}

@Composable
fun WebDiagnosticShell(state: WebStartupState) {
    CompositionLocalProvider(LocalUriHandler provides SecureWebUriHandler) {
        StreamCoreTheme(darkTheme = true) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (state) {
                    WebStartupState.Loading -> LoadingShell()
                    is WebStartupState.BlockingError -> BlockingConfigurationShell(state.guidance)
                    is WebStartupState.Ready -> ReadyShell(state)
                }
            }
        }
    }
}

@Composable
private fun LoadingShell() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(StreamCoreDimens.Spacing.ExtraLarge)
            .testTag(WebDiagnosticTags.Loading),
    ) {
        Text(
            text = "Starting StreamCoreTV web runtime",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text("Loading /config.json before the TMDB graph is created…")
    }
}

@Composable
private fun BlockingConfigurationShell(guidance: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(StreamCoreDimens.Spacing.ExtraLarge)
            .testTag(WebDiagnosticTags.BlockingError),
    ) {
        Text(
            text = "Web runtime configuration is required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(guidance)
        Text(
            "The product graph was not started. Deploy /config.json beside the web distribution, " +
                "using config.example.json as the schema, then reload.",
        )
    }
}

@Composable
private fun ReadyShell(state: WebStartupState.Ready) {
    val route by state.navigationController.route.collectAsState()
    DiagnosticRouteEvidence(route)
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(StreamCoreDimens.Spacing.ExtraLarge)
            .testTag(WebDiagnosticTags.Ready),
    ) {
        Text(
            text = "StreamCoreTV Web Runtime",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "WEB-01 diagnostic shell — no production feature screens or playback engine.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.storageWarning?.let { warning ->
            Text(
                text = warning,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag(WebDiagnosticTags.StorageWarning),
            )
        }
        NavigationProbe(
            route = route,
            onNavigate = state.navigationController::navigate,
        )
        GraphSmokeProbe(state.graph.resolvedDefinitions)
        Text(
            text = "Browser stores: ${state.graph.storageNames.joinToString()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WebImageProbe()
        WebSelectorProbe()
    }
}

@Composable
private fun NavigationProbe(
    route: WebRoute,
    onNavigate: (WebRoute) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
    ) {
        Text(
            text = "Browser history route: ${route.path}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(WebDiagnosticTags.Route),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StreamCoreButton(
                text = "Diagnostic",
                onClick = { onNavigate(WebRoute.Diagnostic) },
                enabled = route != WebRoute.Diagnostic,
                variant = StreamCoreButtonVariant.Secondary,
                modifier = Modifier.testTag(WebDiagnosticTags.RootNavigation),
            )
            StreamCoreButton(
                text = "Details ID",
                onClick = { onNavigate(WebRoute.DiagnosticDetails(DiagnosticContentId)) },
                enabled = route !is WebRoute.DiagnosticDetails,
                variant = StreamCoreButtonVariant.Secondary,
                modifier = Modifier.testTag(WebDiagnosticTags.DetailsNavigation),
            )
            StreamCoreButton(
                text = "Player ID",
                onClick = {
                    prepareDiagnosticPlayerLaunchFromCurrentLocation()
                    onNavigate(WebRoute.DiagnosticPlayer(DiagnosticContentId))
                },
                enabled = route !is WebRoute.DiagnosticPlayer,
                variant = StreamCoreButtonVariant.Secondary,
                modifier = Modifier.testTag(WebDiagnosticTags.PlayerNavigation),
            )
            StreamCoreButton(
                text = "External link",
                onClick = { uriHandler.openUri("https://example.com/web-link-probe") },
                enabled = true,
                variant = StreamCoreButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun GraphSmokeProbe(resolvedDefinitions: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier.testTag(WebDiagnosticTags.Graph),
    ) {
        Text(
            text = "TMDB-only Koin graph: ${resolvedDefinitions.size} definitions resolved",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = resolvedDefinitions.joinToString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val DiagnosticContentId = "603"
