package com.pampoukidis.streamcoretv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.motion.rememberArtworkPreparation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Navigation state helper; no UI to preview. The origin destination owns/cancels preparation. */
@Composable
internal fun rememberPreparedArtworkNavigation(
    onNavigate: (ContentModel, String?) -> Unit,
): (ContentModel, String?) -> Unit {
    val prepareArtwork = rememberArtworkPreparation()
    val currentNavigate by rememberUpdatedState(onNavigate)
    val scope = rememberCoroutineScope()
    val pending = remember { ArtworkNavigationRequest() }
    return remember(scope, prepareArtwork) {
        { content, sourceArtworkUrl ->
            pending.job?.cancel()
            if (sourceArtworkUrl.isNullOrBlank()) {
                currentNavigate(content, sourceArtworkUrl)
            } else {
                pending.job = scope.launch {
                    val targetUrl = content.backdrop?.takeIf { it.isNotBlank() } ?: content.poster
                    val ready = withTimeoutOrNull(ArtworkPreparationTimeoutMillis) {
                        prepareArtwork(sourceArtworkUrl, targetUrl)
                    } == true
                    // Failed preparation keeps A rather than introducing a late surprise image.
                    val snapshot = if (ready) content else content.copy(backdrop = sourceArtworkUrl)
                    currentNavigate(snapshot, sourceArtworkUrl)
                }
            }
        }
    }
}

private class ArtworkNavigationRequest {
    var job: Job? = null
}

private const val ArtworkPreparationTimeoutMillis = 2_000L
