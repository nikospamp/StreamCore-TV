package com.pampoukidis.streamcoretv.feature.profiles.tablet.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel

@Composable
fun TabletProfilesRoute(
    onProfileSelected: (ProfileModel) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    isLogoutInProgress: Boolean,
    onLogoutRequested: () -> Unit,
    onError: (AppError) -> Unit,
    viewModel: ProfilesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
    )

    TabletProfilesScreen(
        state = state,
        onAction = viewModel::onAction,
        onCreateProfile = onCreateProfile,
        onEditProfile = onEditProfile,
        isLogoutInProgress = isLogoutInProgress,
        onLogoutRequested = onLogoutRequested,
    )
}

