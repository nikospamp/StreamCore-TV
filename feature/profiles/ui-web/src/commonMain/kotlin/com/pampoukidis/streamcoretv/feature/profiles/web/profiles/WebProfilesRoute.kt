package com.pampoukidis.streamcoretv.feature.profiles.web.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WebProfilesRoute(
    onProfileSelected: (ProfileModel) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
    viewModel: ProfilesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
    )

    WebProfilesScreen(
        state = state,
        onAction = viewModel::onAction,
        onCreateProfile = onCreateProfile,
        onEditProfile = onEditProfile,
        onBack = onBack,
    )
}
