package com.pampoukidis.streamcoretv.core.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Image-loading helper with no rendered UI to preview. Requests are cancelled with their caller. */
@Composable
fun rememberArtworkPreparation(): suspend (String?, String?) -> Boolean {
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val windowSize = LocalWindowInfo.current.containerSize
    return remember(context, imageLoader, windowSize) {
        { sourceUrl: String?, targetUrl: String? ->
            coroutineScope {
                val width = windowSize.width.coerceAtLeast(1)
                val height = (width * 9 / 16).coerceAtLeast(1)
                val urls = listOfNotNull(sourceUrl, targetUrl).filter { it.isNotBlank() }.distinct()
                val requests = urls.map { url ->
                    async {
                        imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(url)
                                .size(width, height)
                                .crossfade(false)
                                .build(),
                        ) is SuccessResult
                    }
                }
                requests.isNotEmpty() && requests.map { it.await() }.all { it }
            }
        }
    }
}
