package com.pampoukidis.streamcoretv.feature.home.tablet.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeRouteEventEffect
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel

@Composable
fun TabletHomeRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onProfileSelected: () -> Unit,
    onError: (AppError) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: HomeViewModel = koinViewModel(),
    activeProfile: ProfileModel? = null,
    bottomContentPadding: Dp = StreamCoreDimens.Spacing.ExtraLarge,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(profileId, viewModel) {
        viewModel.onAction(HomeAction.Load(profileId))
    }

    HomeRouteEventEffect(
        viewModel = viewModel,
        onContentSelected = onContentSelected,
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
