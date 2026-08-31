package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel

@Composable
fun TvProfilesRoute(
    onProfileSelected: (ProfileModel) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    isLogoutConfirmationVisible: Boolean,
    isLogoutInProgress: Boolean,
    onLogoutRequested: () -> Unit,
    onError: (AppError) -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel(),
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
    )

    TvProfilesScreen(
        state = state,
        onAction = viewModel::onAction,
        onCreateProfile = onCreateProfile,
        onEditProfile = onEditProfile,
        isLogoutConfirmationVisible = isLogoutConfirmationVisible,
        isLogoutInProgress = isLogoutInProgress,
        onLogoutRequested = onLogoutRequested,
        sharedElementScope = sharedElementScope,
    )
}

