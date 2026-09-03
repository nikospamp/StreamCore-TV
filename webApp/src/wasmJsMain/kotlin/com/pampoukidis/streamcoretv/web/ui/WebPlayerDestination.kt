package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebBlockingSurface
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerViewModel
import com.pampoukidis.streamcoretv.feature.player.web.player.WebPlayerRoute
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import kotlinx.coroutines.CancellationException
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WebPlayerDestination(
    profileId: String,
    contentId: String,
    transientRequest: PlaybackRequestModel?,
    detailsRepository: DetailsRepository,
    onBack: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val matchingTransientRequest = transientRequest?.takeIf { request ->
        request.profileId == profileId && request.contentId == contentId
    }
    var resolution by remember(profileId, contentId, matchingTransientRequest) {
        mutableStateOf<WebPlayerResolution>(
            matchingTransientRequest?.let(WebPlayerResolution::Ready)
                ?: WebPlayerResolution.Loading,
        )
    }
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)

    LaunchedEffect(profileId, contentId, matchingTransientRequest, detailsRepository) {
        if (matchingTransientRequest != null) {
            resolution = WebPlayerResolution.Ready(matchingTransientRequest)
            return@LaunchedEffect
        }
        resolution = resolveWebPlaybackRequest(
            profileId = profileId,
            contentId = contentId,
            detailsRepository = detailsRepository,
        )
        if (resolution == WebPlayerResolution.Unavailable) {
            currentOnUnavailable()
        }
    }

    when (val currentResolution = resolution) {
        WebPlayerResolution.Loading -> StreamCoreWebBlockingSurface(
            title = "Preparing playback",
            message = "Resolving this title for the selected profile…",
        )

        WebPlayerResolution.Unavailable -> StreamCoreWebBlockingSurface(
            title = "Playback unavailable",
            message = "Returning to title details…",
        )

        is WebPlayerResolution.Ready -> ResolvedWebPlayer(
            request = currentResolution.request,
            onBack = onBack,
        )
    }
}

@Composable
private fun ResolvedWebPlayer(
    request: PlaybackRequestModel,
    onBack: () -> Unit,
) {
    val storeOwner = remember(request.profileId, request.contentId) {
        WebPlayerViewModelStoreOwner()
    }
    val viewModel = koinViewModel<PlayerViewModel>(
        key = "web-player:${request.profileId}:${request.contentId}",
        viewModelStoreOwner = storeOwner,
    )
    DisposableEffect(storeOwner) {
        onDispose {
            storeOwner.viewModelStore.clear()
        }
    }
    WebPlayerRoute(
        request = request,
        onBack = onBack,
        viewModel = viewModel,
    )
}

internal suspend fun resolveWebPlaybackRequest(
    profileId: String,
    contentId: String,
    detailsRepository: DetailsRepository,
): WebPlayerResolution {
    return try {
        when (val result = detailsRepository.getDetails(profileId, contentId)) {
            is AppResult.Success -> WebPlayerResolution.Ready(
                PlaybackRequestModel(
                    profileId = profileId,
                    contentId = contentId,
                    contentSnapshot = result.value,
                ),
            )

            is AppResult.Failure -> WebPlayerResolution.Unavailable
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        WebPlayerResolution.Unavailable
    }
}

internal sealed interface WebPlayerResolution {
    data object Loading : WebPlayerResolution
    data object Unavailable : WebPlayerResolution
    data class Ready(val request: PlaybackRequestModel) : WebPlayerResolution
}

private class WebPlayerViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
