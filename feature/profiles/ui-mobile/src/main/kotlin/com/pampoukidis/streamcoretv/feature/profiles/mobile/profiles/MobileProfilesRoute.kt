package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesRouteEventEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesViewModel

@Composable
fun MobileProfilesRoute(
    onProfileSelected: (StreamCoreProfile) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    isLogoutInProgress: Boolean,
    onLogoutRequested: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    autoEnterSingleProfile: Boolean = false,
    onEntryStarted: () -> Unit = {},
    viewModel: ProfilesViewModel = koinViewModel(parameters = { parametersOf(autoEnterSingleProfile) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.pin != null) { viewModel.onAction(ProfilesAction.CancelPin) }

    ProfilesRouteEventEffect(
        viewModel = viewModel,
        onProfileSelected = onProfileSelected,
        onError = onError,
        onEntryStarted = onEntryStarted,
    )

    BackHandler(enabled = state.mode == ProfilesMode.Manage) {
        viewModel.onAction(ProfilesAction.DoneManaging)
    }

    MobileProfilesScreen(
        state = state,
        onAction = viewModel::onAction,
        onCreateProfile = onCreateProfile,
        onEditProfile = onEditProfile,
        isLogoutInProgress = isLogoutInProgress,
        onLogoutRequested = onLogoutRequested,
        sharedElementScope = sharedElementScope,
    )
}
