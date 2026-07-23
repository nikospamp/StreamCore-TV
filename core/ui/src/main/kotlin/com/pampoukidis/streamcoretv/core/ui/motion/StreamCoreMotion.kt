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
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Scopes shared-transition modifiers to the current navigation transition.
 *
 * The navigation layer creates one for each destination from the enclosing [SharedTransitionScope]
 * and that destination's [AnimatedVisibilityScope]. Feature UI may receive `null` when no shared
 * transition is available, including previews; the motion modifiers then degrade to their
 * documented non-animated behavior.
 */
class StreamCoreSharedElementScope(
    internal val sharedTransitionScope: SharedTransitionScope,
    internal val animatedVisibilityScope: AnimatedVisibilityScope,
)

/**
 * Animates matching content between navigation destinations while preserving [clipShape].
 *
 * [key] must identify the same visual element in both destinations. Prefer
 * [StreamCoreSharedKey.artwork] or [StreamCoreSharedKey.title] over constructing keys at the call
 * site. When [sharedElementScope] is `null`, this modifier only applies [clipShape].
 *
 * Use a value from [StreamCoreSharedElementZIndex] for [zIndexInOverlay]. Elements that must appear
 * together during the transition should use the same semantic layer in both destinations.
 */
@Composable
fun Modifier.streamCoreSharedBounds(
    sharedElementScope: StreamCoreSharedElementScope?,
    key: String,
    clipShape: Shape,
    zIndexInOverlay: Float = StreamCoreSharedElementZIndex.Artwork,
): Modifier {
    if (sharedElementScope == null) {
        return this.clip(clipShape)
    }

    return with(sharedElementScope.sharedTransitionScope) {
        this@streamCoreSharedBounds
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = sharedElementScope.animatedVisibilityScope,
                enter = fadeIn(
                    animationSpec = tween(StreamCoreMotionDurations.SharedElementFadeMillis),
                ),
                exit = fadeOut(
                    animationSpec = tween(StreamCoreMotionDurations.SharedElementFadeMillis),
                ),
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.Crop,
                ),
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = OverlayClip(clipShape),
            )
            .clip(clipShape)
    }
}

/**
 * Keeps supporting card content visible above shared bounds for the duration of a transition.
 *
 * This is intended for non-shared visuals such as scrims, metadata, progress, and rank badges that
 * would otherwise be obscured by the shared-transition overlay. The content uses the navigation
 * fade timings so it remains synchronized with its destination. When [sharedElementScope] is
 * `null`, this modifier is a no-op.
 *
 * [zIndexInOverlay] is deliberately required. Choose the semantic layer from
 * [StreamCoreSharedElementZIndex] instead of using an arbitrary value.
 *
 * Content rendered in the shared-transition overlay does not inherit clipping from its original
 * parent. Pass [clipShape] for painted content such as gradients; it is applied inside the overlay
 * before any modifiers appended by the caller.
 */
@Composable
fun Modifier.streamCoreOverlayDuringSharedTransition(
    sharedElementScope: StreamCoreSharedElementScope?,
    zIndexInOverlay: Float,
    clipShape: Shape? = null,
): Modifier {
    if (sharedElementScope == null) {
        return this
    }

    val overlayModifier = with(sharedElementScope.sharedTransitionScope) {
        with(sharedElementScope.animatedVisibilityScope) {
            this@streamCoreOverlayDuringSharedTransition
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = zIndexInOverlay,
                )
                .animateEnterExit(
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = StreamCoreMotionDurations.NavigationEnterMillis,
                            delayMillis = StreamCoreMotionDurations.NavigationEnterDelayMillis,
                        ),
                    ),
                    exit = fadeOut(
                        animationSpec = tween(StreamCoreMotionDurations.NavigationExitMillis),
                    ),
                )
        }
    }

    return if (clipShape != null) {
        overlayModifier.clip(clipShape)
    } else {
        overlayModifier
    }
}

/**
 * Reveals [content] with the standard delayed fade-and-rise entrance.
 *
 * A change to [visibleKey] restarts the entrance. Non-positive [delayMillis] values start it
 * immediately. Previews render [content] immediately so they remain deterministic.
 */
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

/** Creates stable, matching keys for content participating in shared transitions. */
object StreamCoreSharedKey {
    /** Returns the shared-transition key for a profile avatar. */
    fun profileAvatar(profileId: String): String {
        return "profile:$profileId:avatar"
    }

    /**
     * Identifies a content item and its source row independently of a visual element.
     *
     * [row] is included because the same content may appear in more than one home row. A blank or
     * missing row uses a deterministic fallback for non-row surfaces.
     */
    fun content(
        contentId: String,
        row: String?,
    ): String {
        val rowKey = row?.takeIf { value -> value.isNotBlank() } ?: NoRow
        return "$contentId:$rowKey"
    }

    /** Returns the shared-transition key for a content item's artwork. */
    fun artwork(
        contentId: String,
        row: String?,
    ): String {
        return "asset:${content(contentId = contentId, row = row)}:artwork"
    }

    /** Returns the shared-transition key for a content item's title. */
    fun title(
        contentId: String,
        row: String?,
    ): String {
        return "asset:${content(contentId = contentId, row = row)}:title"
    }

    private const val NoRow = "no-row"
}

/**
 * Semantic drawing order for content rendered in the shared-transition overlay.
 *
 * Keep these values centralized and use them instead of local z-index values. Higher layers render
 * above lower layers.
 */
object StreamCoreSharedElementZIndex {
    /** Shared artwork and other base transition elements. */
    const val Artwork = 0f

    /** Gradients and contrast treatments rendered over artwork. */
    const val Scrim = 1f

    /** Titles, metadata, and progress indicators. */
    const val Content = 2f

    /** Visuals that must remain above all card content, such as rank badges. */
    const val Foreground = 3f
}

/**
 * Timing contract shared by navigation and shared-element overlays.
 *
 * These values intentionally stay public so the app navigation host and feature overlays use one
 * synchronized source of truth. Do not override one side of that transition independently.
 */
object StreamCoreMotionDurations {
    /** Navigation container slide duration. */
    const val NavigationSlideMillis = 240

    /** Incoming destination and overlay fade-in duration. */
    const val NavigationEnterMillis = 140

    /** Outgoing destination and overlay fade-out duration. */
    const val NavigationExitMillis = 90

    /** Delay before the incoming destination and overlay begin fading in. */
    const val NavigationEnterDelayMillis = 20

    /** Fade duration for content participating directly in shared bounds. */
    const val SharedElementFadeMillis = 90
}

private const val EntranceTransformMillis = 220
private val EntranceOffset = StreamCoreDimens.Motion.EntranceOffset
