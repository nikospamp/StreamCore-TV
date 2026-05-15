package com.pampoukidis.streamcoretv.feature.profiles.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.feature.profiles.common.effects.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.presentation.ProfilesViewModel

@Composable
fun MobileProfilesRoute(
    onProfileSelected: (ProfileModel) -> Unit,
    onError: (AppError) -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
    )

    MobileProfilesScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
