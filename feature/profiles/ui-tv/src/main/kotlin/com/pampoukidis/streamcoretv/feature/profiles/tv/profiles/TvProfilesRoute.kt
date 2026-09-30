package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.runtime.Composable
import org.koin.core.parameter.parametersOf
import androidx.activity.compose.BackHandler
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel

@Composable
fun TvProfilesRoute(
    onProfileSelected: (StreamCoreProfile) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    isLogoutConfirmationVisible: Boolean,
    isLogoutInProgress: Boolean,
    onLogoutRequested: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    autoEnterSingleProfile: Boolean = false,
    onEntryStarted: () -> Unit = {},
    viewModel: ProfilesViewModel = koinViewModel(parameters = { parametersOf(autoEnterSingleProfile) }),
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.pin != null) { viewModel.onAction(ProfilesAction.CancelPin) }

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
        onEntryStarted = onEntryStarted,
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

