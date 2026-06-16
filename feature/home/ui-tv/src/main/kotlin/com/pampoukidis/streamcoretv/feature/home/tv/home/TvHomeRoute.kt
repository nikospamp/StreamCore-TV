package com.pampoukidis.streamcoretv.feature.home.tv.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeRouteEventEffect
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeViewModel

@Composable
fun TvHomeRoute(
    profileId: String,
    selectedContentKey: String?,
    onContentSelected: (ContentModel) -> Unit,
    onError: (AppError) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    viewModel: HomeViewModel = hiltViewModel(),
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

    TvHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
    )
}
