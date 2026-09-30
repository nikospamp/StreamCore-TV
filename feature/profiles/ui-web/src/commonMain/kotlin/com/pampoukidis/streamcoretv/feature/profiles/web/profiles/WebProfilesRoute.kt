package com.pampoukidis.streamcoretv.feature.profiles.web.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebProfilesRoute(
    profilesRevision: Int,
    onProfileSelected: (StreamCoreProfile) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onBack: () -> Unit,
    onProfilesLoaded: (List<StreamCoreProfile>) -> Unit = {},
    onError: (StreamCoreError) -> Unit,
    autoEnterSingleProfile: Boolean = false,
    onEntryStarted: () -> Unit = {},
    onPinBackHandlerChanged: ((() -> Unit)?) -> Unit = {},
    viewModel: ProfilesViewModel = koinViewModel(parameters = { parametersOf(autoEnterSingleProfile) }),
    onLogoutRequested: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val currentPinBackHandlerChanged by rememberUpdatedState(onPinBackHandlerChanged)
    DisposableEffect(state.pin?.challengeId, viewModel) {
        currentPinBackHandlerChanged(if (state.pin != null) { { viewModel.onAction(ProfilesAction.CancelPin) } } else null)
        onDispose { currentPinBackHandlerChanged(null) }
    }

    LaunchedEffect(profilesRevision, viewModel) {
        viewModel.onAction(ProfilesAction.RouteEntered)
    }

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
        onEntryStarted = onEntryStarted,
    )

    LaunchedEffect(state.isLoading, state.loadError, state.profiles) {
        if (!state.isLoading && state.loadError == null) {
            onProfilesLoaded(state.profiles)
        }
    }

    WebProfilesScreen(
        state = state,
        onAction = viewModel::onAction,
        onCreateProfile = onCreateProfile,
        onEditProfile = onEditProfile,
        onBack = onBack,
        onLogoutRequested = onLogoutRequested,
    )
}
