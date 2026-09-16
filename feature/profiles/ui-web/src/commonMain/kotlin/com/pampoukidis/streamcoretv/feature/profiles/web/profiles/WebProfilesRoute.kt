package com.pampoukidis.streamcoretv.feature.profiles.web.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebProfilesRoute(
    profilesRevision: Int,
    onProfileSelected: (ProfileModel) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onBack: () -> Unit,
    onProfilesLoaded: (List<ProfileModel>) -> Unit = {},
    onError: (AppError) -> Unit,
    viewModel: ProfilesViewModel = koinViewModel(),
    onLogoutRequested: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profilesRevision, viewModel) {
        if (profilesRevision > 0) {
            viewModel.onAction(ProfilesAction.Refresh)
        }
    }

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
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
