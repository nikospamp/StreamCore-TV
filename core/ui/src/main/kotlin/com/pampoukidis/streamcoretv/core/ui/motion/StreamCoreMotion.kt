package com.pampoukidis.streamcoretv.core.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class StreamCoreSharedElementScope(
    internal val sharedTransitionScope: SharedTransitionScope,
    internal val animatedVisibilityScope: AnimatedVisibilityScope,
)

@Composable
fun Modifier.streamCoreSharedBounds(
    sharedElementScope: StreamCoreSharedElementScope?,
    key: String,
    clipShape: Shape,
): Modifier {
    if (sharedElementScope == null) {
        return this.clip(clipShape)
    }

    return with(sharedElementScope.sharedTransitionScope) {
        this@streamCoreSharedBounds
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = sharedElementScope.animatedVisibilityScope,
                enter = fadeIn(animationSpec = tween(SharedElementFadeMillis)),
                exit = fadeOut(animationSpec = tween(SharedElementFadeMillis)),
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.Crop,
                ),
                clipInOverlayDuringTransition = OverlayClip(clipShape),
            )
            .clip(clipShape)
    }
}

@Composable
fun StreamCoreDelayedEntrance(
    visibleKey: Any?,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isInPreview = LocalInspectionMode.current
    var visible by remember(visibleKey, isInPreview) {
        mutableStateOf(isInPreview)
    }
    val entranceProgress = animateFloatAsState(
        targetValue = if (visible) {
            1f
        } else {
            0f
        },
        animationSpec = tween(
            durationMillis = EntranceTransformMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "StreamCoreDelayedEntrance",
    )
    val initialTranslationY = with(LocalDensity.current) {
        EntranceOffset.toPx()
    }

    if (!isInPreview) {
        LaunchedEffect(visibleKey, delayMillis) {
            if (delayMillis > 0) {
                delay(delayMillis.toLong().milliseconds)
            }
            visible = true
        }
    }

    if (visible) {
        Box(
            modifier = modifier.graphicsLayer {
                val progress = entranceProgress.value
                alpha = progress
                translationY = (1f - progress) * initialTranslationY
            },
        ) {
            content()
        }
    }
}

fun streamCoreContentSharedIdentity(
    contentId: String,
    row: String?,
): String {
    val rowKey = row?.takeIf { value -> value.isNotBlank() } ?: NoRowSharedKey
    return "$contentId:$rowKey"
}

fun streamCoreArtworkSharedKey(
    contentId: String,
    row: String?,
): String {
    return "asset:${streamCoreContentSharedIdentity(contentId = contentId, row = row)}:artwork"
}

fun streamCoreTitleSharedKey(
    contentId: String,
    row: String?,
): String {
    return "asset:${streamCoreContentSharedIdentity(contentId = contentId, row = row)}:title"
}

private const val SharedElementFadeMillis = 90
private const val EntranceTransformMillis = 220
private const val NoRowSharedKey = "no-row"
private val EntranceOffset = 80.dp
