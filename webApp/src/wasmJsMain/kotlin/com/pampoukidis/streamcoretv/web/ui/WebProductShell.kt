package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.avatar.LocalProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebBlockingSurface
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebBrowseScaffold
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.feature.login.web.login.WebLoginRoute
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.web.editor.WebProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.web.profiles.WebProfilesRoute
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import com.pampoukidis.streamcoretv.web.navigation.isDiagnosticRoute
import com.pampoukidis.streamcoretv.web.platform.SecureWebUriHandler
import com.pampoukidis.streamcoretv.web.product.WebProductCoordinator
import com.pampoukidis.streamcoretv.web.product.WebProductInitialization
import com.pampoukidis.streamcoretv.web.startup.WebStartupState
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinIsolatedContext
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import streamcoretv.core.ui.generated.resources.Res

@Composable
fun WebProductShell(state: WebStartupState) {
    CompositionLocalProvider(LocalUriHandler provides SecureWebUriHandler) {
        StreamCoreTheme(darkTheme = true) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (state) {
                    WebStartupState.Loading -> StreamCoreWebBlockingSurface(
                        title = "Starting StreamCoreTV",
                        message = "Restoring your secure browser session…",
                    )
                    is WebStartupState.BlockingError -> StreamCoreWebBlockingSurface(
                        title = "Web configuration required",
                        message = state.guidance,
                    )
                    is WebStartupState.Ready -> {
                        val navigationEntry by state.navigationController.entry.collectAsState()
                        if (navigationEntry.route.isDiagnosticRoute()) {
                            WebDiagnosticShell(state)
                        } else {
                            KoinIsolatedContext(state.graph.application) {
                                ReadyProductShell(state)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyProductShell(state: WebStartupState.Ready) {
    val navigationEntry by state.navigationController.entry.collectAsState()
    val route = navigationEntry.route
    val scope = rememberCoroutineScope()
    val coordinator = remember(state.graph, state.navigationController) {
        WebProductCoordinator(
            koin = state.graph.application.koin,
            navigation = state.navigationController,
        )
    }
    val avatarResolver = remember(state.graph) {
        state.graph.application.koin.get<ProfileAvatarArtworkResolver>()
    }
    var initializing by remember { mutableStateOf(true) }
    var activeError by remember { mutableStateOf<AppError?>(null) }
    var profilesRevision by remember { mutableIntStateOf(0) }
    var logoutInProgress by remember { mutableStateOf(false) }
    val handleProductError: (AppError) -> Unit = { error ->
        activeError = error
        document.body?.setAttribute("data-product-error-kind", error.webErrorKind())
        scope.launch { coordinator.handleError(error) }
    }
    val profileChanged: () -> Unit = {
        profilesRevision += 1
        scope.launch {
            val error = coordinator.reconcileProfilesFromRepository()
            if (error == null) {
                state.navigationController.navigate(WebRoute.Profiles)
            } else {
                handleProductError(error)
            }
        }
    }
    val closeProfileEditor: () -> Unit = {
        state.navigationController.navigate(WebRoute.Profiles)
    }
    val changeProfile: () -> Unit = {
        scope.launch { coordinator.changeProfile() }
    }
    val logout: () -> Unit = {
        if (!logoutInProgress) {
            logoutInProgress = true
            scope.launch {
                try {
                    coordinator.logout()?.let { error -> activeError = error }
                } finally {
                    logoutInProgress = false
                }
            }
        }
    }

    LaunchedEffect(coordinator) {
        when (val result = coordinator.initialize()) {
            WebProductInitialization.Ready -> Unit
            is WebProductInitialization.ReadyWithError -> activeError = result.error
        }
        initializing = false
    }

    LaunchedEffect(route, initializing) {
        if (!initializing) {
            val canonicalRoute = coordinator.canonicalRoute(route)
            coordinator.sanitizeRoute(route)
            document.body?.setAttribute("data-product-route", canonicalRoute.path)
            document.body?.setAttribute("data-product-visual-state", "loading")
            repeat(VISUAL_SETTLE_FRAMES) {
                androidx.compose.runtime.withFrameNanos { }
            }
            delay(VISUAL_SETTLE_DELAY_MILLIS)
            document.body?.setAttribute("data-product-visual-state", "ready")
        }
    }

    CompositionLocalProvider(LocalProfileAvatarArtworkResolver provides avatarResolver) {
        if (initializing) {
            StreamCoreWebBlockingSurface(
                title = "Restoring session",
                message = "Validating your account and selected profile…",
            )
        } else {
            when (val destination = coordinator.canonicalRoute(route)) {
                WebRoute.Root -> Unit
                WebRoute.Login -> WebLoginRoute(
                    onLoginSucceeded = {
                        scope.launch { coordinator.loginSucceeded() }
                    },
                    onForgotPassword = {},
                    onCreateAccount = {},
                    onHelp = {},
                    onError = handleProductError,
                )
                WebRoute.Profiles -> WebProfilesRoute(
                    profilesRevision = profilesRevision,
                    onProfileSelected = { profile ->
                        scope.launch { coordinator.profileSelected(profile) }
                    },
                    onCreateProfile = { state.navigationController.navigate(WebRoute.CreateProfile) },
                    onEditProfile = { profileId ->
                        state.navigationController.navigate(WebRoute.EditProfile(profileId))
                    },
                    onBack = {},
                    onProfilesLoaded = { profiles ->
                        document.body?.setAttribute("data-profile-count", profiles.size.toString())
                        scope.launch { coordinator.reconcileProfiles(profiles) }
                    },
                    onError = handleProductError,
                )
                WebRoute.CreateProfile -> WebProfileEditorRoute(
                    mode = ProfileEditorMode.Create,
                    profileId = null,
                    onProfileChanged = profileChanged,
                    onClose = closeProfileEditor,
                    onError = handleProductError,
                )
                is WebRoute.EditProfile -> WebProfileEditorRoute(
                    mode = ProfileEditorMode.Edit,
                    profileId = destination.profileId,
                    onProfileChanged = profileChanged,
                    onClose = closeProfileEditor,
                    onError = handleProductError,
                )
                WebRoute.Home -> WebBrowsePlaceholder(
                    destination = WebBrowseDestination.Home,
                    title = "Home",
                    message = "Home is ready for its WEB-03B feature implementation.",
                    profileName = coordinator.selectedProfile?.displayName.orEmpty(),
                    logoutInProgress = logoutInProgress,
                    onNavigate = state.navigationController::navigate,
                    onChangeProfile = changeProfile,
                    onLogout = logout,
                )
                WebRoute.Search -> WebBrowsePlaceholder(
                    destination = WebBrowseDestination.Search,
                    title = "Search",
                    message = "Search is ready for its WEB-03C feature implementation.",
                    profileName = coordinator.selectedProfile?.displayName.orEmpty(),
                    logoutInProgress = logoutInProgress,
                    onNavigate = state.navigationController::navigate,
                    onChangeProfile = changeProfile,
                    onLogout = logout,
                )
                WebRoute.Library -> WebBrowsePlaceholder(
                    destination = WebBrowseDestination.Library,
                    title = "Library",
                    message = "Library is ready for its WEB-03D feature implementation.",
                    profileName = coordinator.selectedProfile?.displayName.orEmpty(),
                    logoutInProgress = logoutInProgress,
                    onNavigate = state.navigationController::navigate,
                    onChangeProfile = changeProfile,
                    onLogout = logout,
                )
                is WebRoute.Details -> WebBrowsePlaceholder(
                    destination = WebBrowseDestination.Details,
                    title = "Details",
                    message = "Content ${destination.contentId} is ready for its WEB-03E feature implementation.",
                    profileName = coordinator.selectedProfile?.displayName.orEmpty(),
                    logoutInProgress = logoutInProgress,
                    onNavigate = state.navigationController::navigate,
                    onChangeProfile = changeProfile,
                    onLogout = logout,
                )
                is WebRoute.Player -> WebBrowsePlaceholder(
                    destination = null,
                    title = "Playback is not available yet",
                    message = "This WEB-03 route is an explicit placeholder. No media is loaded.",
                    profileName = coordinator.selectedProfile?.displayName.orEmpty(),
                    logoutInProgress = logoutInProgress,
                    onNavigate = state.navigationController::navigate,
                    onChangeProfile = changeProfile,
                    onLogout = logout,
                    actionLabel = "Back to details",
                    onAction = {
                        state.navigationController.replace(WebRoute.Details(destination.contentId))
                    },
                )
                WebRoute.AuthenticatedLanding -> Unit
                WebRoute.Diagnostic,
                is WebRoute.DiagnosticDetails,
                is WebRoute.DiagnosticPlayer -> WebDiagnosticShell(state)
            }
        }
    }

    activeError?.let { error ->
        WebErrorDialog(
            error = error,
            mapper = state.graph.application.koin.get(),
            onDismiss = { activeError = null },
        )
    }
}

private const val VISUAL_SETTLE_FRAMES = 3
private const val VISUAL_SETTLE_DELAY_MILLIS = 1_500L

@Composable
private fun WebBrowsePlaceholder(
    destination: WebBrowseDestination?,
    title: String,
    message: String,
    profileName: String,
    logoutInProgress: Boolean,
    onNavigate: (WebRoute) -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StreamCoreWebBrowseScaffold(
        activeDestination = destination,
        profileName = profileName,
        logoutInProgress = logoutInProgress,
        onDestinationSelected = { selected -> onNavigate(selected.toRoute()) },
        onChangeProfile = onChangeProfile,
        onLogout = onLogout,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            StreamCoreWebPanel {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                ) {
                    Text(title, style = MaterialTheme.typography.displaySmall)
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (actionLabel != null && onAction != null) {
                        StreamCoreWebButton(text = actionLabel, onClick = onAction)
                    }
                }
            }
        }
    }
}

private fun WebBrowseDestination.toRoute(): WebRoute {
    return when (this) {
        WebBrowseDestination.Home -> WebRoute.Home
        WebBrowseDestination.Search -> WebRoute.Search
        WebBrowseDestination.Library -> WebRoute.Library
        WebBrowseDestination.Details -> WebRoute.Home
    }
}

@Composable
private fun WebErrorDialog(
    error: AppError,
    mapper: ErrorPresentationMapper,
    onDismiss: () -> Unit,
) {
    val presentation = mapper.map(error)
    val errorKind = error.webErrorKind()
    val title = stringResource(presentation.title)
    val message = stringResource(presentation.message)
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    DisposableEffect(errorKind, title, message, presentation.dismissible) {
        val escapeListener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            if (presentation.dismissible && keyboardEvent?.key == "Escape") {
                keyboardEvent.preventDefault()
                currentOnDismiss.value()
            }
        }
        document.body?.setAttribute("data-product-error-kind", errorKind)
        document.body?.setAttribute("data-product-error-title", title)
        document.body?.setAttribute("data-product-error-message", message)
        window.addEventListener("keydown", escapeListener)
        onDispose {
            window.removeEventListener("keydown", escapeListener)
            document.body?.removeAttribute("data-product-error-kind")
            document.body?.removeAttribute("data-product-error-title")
            document.body?.removeAttribute("data-product-error-message")
        }
    }
    Dialog(onDismissRequest = { if (presentation.dismissible) onDismiss() }) {
        StreamCoreWebPanel(
            modifier = Modifier.semantics {
                contentDescription = "$title. $message"
                error(message)
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StreamCoreWebButton(
                    text = stringResource(presentation.confirmAction),
                    onClick = onDismiss,
                )
            }
        }
    }
}

private fun AppError.webErrorKind(): String {
    return when (this) {
        is AppError.Authentication -> "authentication"
        is AppError.Network -> "network"
        is AppError.Parsing -> "parsing"
        is AppError.Server -> "server"
        is AppError.SessionExpired -> "session-expired"
        is AppError.Timeout -> "timeout"
        is AppError.Unauthorized -> "unauthorized"
        is AppError.Unknown -> "unknown"
    }
}
