package com.pampoukidis.streamcoretv.feature.home.mobile.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeRouteEventEffect
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel

@Composable
fun MobileHomeRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (StreamCoreContent) -> Unit,
    onContentArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
    onProfileSelected: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    bottomContentPadding: Dp,
    activeProfile: StreamCoreProfile? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(HomeAction.Load(profileId))
    }

    HomeRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = onContentSelected,
        onContentArtworkSelected = onContentArtworkSelected,
        onError = onError,
    )

    MobileHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onProfileSelected = onProfileSelected,
        bottomContentPadding = bottomContentPadding,
        activeProfile = activeProfile,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
    )
}
