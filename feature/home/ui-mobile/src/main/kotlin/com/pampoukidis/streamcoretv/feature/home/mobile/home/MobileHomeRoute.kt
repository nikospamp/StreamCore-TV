package com.pampoukidis.streamcoretv.feature.home.mobile.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeRouteEventEffect
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel

@Composable
fun MobileHomeRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onContentArtworkSelected: ((ContentModel, String?) -> Unit)? = null,
    onProfileSelected: () -> Unit,
    onError: (AppError) -> Unit,
    bottomContentPadding: Dp,
    activeProfile: ProfileModel? = null,
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
