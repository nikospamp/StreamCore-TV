package com.pampoukidis.streamcoretv.feature.home.tablet.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeRouteEventEffect
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel

@Composable
fun TabletHomeRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (StreamCoreContent) -> Unit,
    onContentArtworkSelected: ((StreamCoreContent, String?) -> Unit)? = null,
    onProfileSelected: () -> Unit,
    onError: (StreamCoreError) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: HomeViewModel = koinViewModel(),
    activeProfile: StreamCoreProfile? = null,
    bottomContentPadding: Dp = StreamCoreDimens.Spacing.ExtraLarge,
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

    TabletHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onProfileSelected = onProfileSelected,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
        activeProfile = activeProfile,
        bottomContentPadding = bottomContentPadding,
    )
}
