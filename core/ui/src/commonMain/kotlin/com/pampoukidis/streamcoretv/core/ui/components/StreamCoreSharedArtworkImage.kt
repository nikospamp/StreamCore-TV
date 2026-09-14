package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.compose.rememberAsyncImagePainter
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreMotionDurations
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedArtworkBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import kotlin.math.roundToInt

/**
 * Draws artwork and normalized [scrims] in one shared leaf. Text and controls belong outside it.
 *
 * Requests use the endpoint's stable size before shared bounds, never the animated drawing size.
 * For Details, [sourceImageUrl] is the exact clicked artwork. It blends to preloaded [imageUrl]
 * during the same navigation transition. Both images
 * interpolate their crop from center to [alignment] on that progress. Image identities are frozen
 * for the route so a later decode cannot replace the picture at the end of movement.
 */
@Composable
fun StreamCoreSharedArtworkImage(
    imageUrl: String?,
    contentDescription: String?,
    fallbackText: String,
    sharedKey: String,
    clipShape: Shape,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    sourceImageUrl: String? = null,
    alignment: Alignment = Alignment.Center,
    scrims: List<Brush> = emptyList(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fallbackTextStyle: TextStyle = MaterialTheme.typography.displayMedium,
) {
    val sourceUrl = sourceImageUrl?.takeIf { value -> value.isNotBlank() }
    val targetUrl = if (sourceUrl == null) {
        imageUrl?.takeIf { value -> value.isNotBlank() }
    } else {
        remember(sharedKey, sourceUrl) { imageUrl?.takeIf { value -> value.isNotBlank() } }
    }
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) { SingletonImageLoader.get(platformContext) }
    var endpointSize by remember(sharedKey) { mutableStateOf(IntSize.Zero) }
    val targetPainter = rememberArtworkPainter(targetUrl, endpointSize, imageLoader)
    val targetState = key(targetPainter) { targetPainter?.state?.collectAsState()?.value }
    val targetSuccessPainter = (targetState as? AsyncImagePainter.State.Success)
        ?.takeIf { state -> state.result.request.data == targetUrl }
        ?.painter
    val cachedTargetPainter = if (targetSuccessPainter == null && targetUrl != null) {
        remember(imageLoader, targetUrl, platformContext) {
            imageLoader.memoryCache?.get(MemoryCache.Key(targetUrl))?.image?.asPainter(platformContext)
        }
    } else {
        null
    }
    val currentTargetPainter = targetSuccessPainter ?: cachedTargetPainter ?: targetState?.painter
    val paintState = if (sourceUrl == null) {
        // Ordinary cards do not need handoff state, effects, or a second painter publication.
        ArtworkPaintState(basePainter = currentTargetPainter, baseAlignment = alignment)
    } else {
        rememberArtworkHandoff(
            sharedKey = sharedKey,
            sourceUrl = sourceUrl,
            targetUrl = targetUrl,
            endpointSize = endpointSize,
            imageLoader = imageLoader,
            targetSuccessPainter = targetSuccessPainter,
            initialTargetPainter = cachedTargetPainter ?: targetSuccessPainter,
            alignment = alignment,
            sharedElementScope = sharedElementScope,
        )
    }
    val fallbackLayout = if (paintState.basePainter == null) {
        val textMeasurer = rememberTextMeasurer(cacheSize = 0)
        remember(fallbackText, fallbackTextStyle, contentColor, textMeasurer) {
            textMeasurer.measure(
                text = fallbackText,
                style = fallbackTextStyle.copy(color = contentColor),
            )
        }
    } else {
        null
    }
    val descriptionModifier = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }
    Canvas(
        modifier = modifier
            // ContentSize is the endpoint placeholder outside shared bounds, not the animated leaf.
            .onSizeChanged { size -> endpointSize = size }
            .then(descriptionModifier)
            .streamCoreSharedArtworkBounds(
                sharedElementScope = sharedElementScope,
                key = sharedKey,
                imageUrl = sourceUrl ?: targetUrl,
                clipShape = clipShape,
            ),
    ) {
        val cropProgress = paintState.alignmentProgress?.value ?: 1f
        drawRect(containerColor)
        if (fallbackLayout != null) {
            drawText(
                textLayoutResult = fallbackLayout,
                topLeft = Offset(
                    x = (size.width - fallbackLayout.size.width) / 2f,
                    y = (size.height - fallbackLayout.size.height) / 2f,
                ),
            )
        }
        drawCroppedPainter(
            painter = paintState.basePainter,
            alignment = paintState.baseAlignment,
            alignmentProgress = cropProgress,
        )
        if (paintState.overlayPainter != null) {
            drawCroppedPainter(
                painter = paintState.overlayPainter,
                alignment = alignment,
                alpha = (paintState.overlayAlpha?.value ?: 0f) *
                    (paintState.overlayAlphaMultiplier?.value ?: 1f),
                alignmentProgress = cropProgress,
            )
        }
        scrims.forEach { brush -> drawRect(brush) }
    }
}

@Composable
private fun rememberArtworkHandoff(
    sharedKey: String,
    sourceUrl: String,
    targetUrl: String?,
    endpointSize: IntSize,
    imageLoader: ImageLoader,
    targetSuccessPainter: Painter?,
    initialTargetPainter: Painter?,
    alignment: Alignment,
    sharedElementScope: StreamCoreSharedElementScope?,
): ArtworkPaintState {
    val transition = sharedElementScope?.animatedVisibilityScope?.transition
    val navigationProgress = if (transition == null) {
        remember { mutableStateOf(1f) }
    } else {
        transition.animateFloat(
            transitionSpec = {
                tween(StreamCoreMotionDurations.ArtworkTransitionMillis, easing = FastOutSlowInEasing)
            },
            label = "ArtworkNavigationProgress",
        ) { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
    }
    var capturedTargetPainter by remember(sharedKey, sourceUrl) { mutableStateOf(initialTargetPainter) }
    val targetWasPreloaded = remember(sharedKey, sourceUrl) { initialTargetPainter != null }
    val isExiting = transition?.targetState == EnterExitState.PostExit
    val isMoving = transition != null && transition.currentState != transition.targetState
    var fallbackFinished by remember(sharedKey, sourceUrl) { mutableStateOf(targetWasPreloaded) }
    val lateImageAlpha = if (targetWasPreloaded) {
        null
    } else {
        remember(sharedKey, sourceUrl) { Animatable(0f) }
    }
    if (!targetWasPreloaded) {
        LaunchedEffect(targetSuccessPainter) {
            if (capturedTargetPainter == null && targetSuccessPainter != null) {
                capturedTargetPainter = targetSuccessPainter
            }
        }
        LaunchedEffect(capturedTargetPainter, isExiting) {
            if (capturedTargetPainter != null && !isExiting && lateImageAlpha != null) {
                lateImageAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        StreamCoreMotionDurations.ArtworkTransitionMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                fallbackFinished = true
            }
        }
    }
    val sameImage = targetWasPreloaded && sourceUrl == targetUrl && capturedTargetPainter != null
    val keepSource = !sameImage && (isMoving || isExiting || capturedTargetPainter == null || !fallbackFinished)
    val sourcePainter = if (keepSource) {
        key(sharedKey, sourceUrl) {
            rememberFrozenArtworkPainter(sourceUrl, endpointSize, imageLoader)
        }
    } else {
        null
    }
    return if (sameImage || !keepSource || sourcePainter == null) {
        ArtworkPaintState(
            basePainter = capturedTargetPainter ?: sourcePainter,
            baseAlignment = alignment,
            alignmentProgress = navigationProgress,
        )
    } else {
        ArtworkPaintState(
            basePainter = sourcePainter,
            baseAlignment = alignment,
            overlayPainter = capturedTargetPainter,
            overlayAlpha = navigationProgress,
            overlayAlphaMultiplier = lateImageAlpha?.asState(),
            alignmentProgress = navigationProgress,
        )
    }
}

@Composable
private fun rememberFrozenArtworkPainter(
    imageUrl: String,
    decodeSize: IntSize,
    imageLoader: ImageLoader,
): Painter? {
    val platformContext = LocalPlatformContext.current
    var capturedPainter by remember(imageLoader, imageUrl, platformContext) {
        mutableStateOf(imageLoader.memoryCache?.get(MemoryCache.Key(imageUrl))?.image?.asPainter(platformContext))
    }
    if (capturedPainter == null) {
        val painter = rememberArtworkPainter(imageUrl, decodeSize, imageLoader)
        val state = key(painter) { painter?.state?.collectAsState()?.value }
        val loadedPainter = (state as? AsyncImagePainter.State.Success)?.painter
        LaunchedEffect(loadedPainter) {
            if (capturedPainter == null && loadedPainter != null) {
                capturedPainter = loadedPainter
            }
        }
    }
    return capturedPainter
}

private class ArtworkPaintState(
    val basePainter: Painter?,
    val baseAlignment: Alignment,
    val overlayPainter: Painter? = null,
    val overlayAlpha: State<Float>? = null,
    val overlayAlphaMultiplier: State<Float>? = null,
    val alignmentProgress: State<Float>? = null,
)

@Composable
private fun rememberArtworkPainter(
    imageUrl: String?,
    decodeSize: IntSize,
    imageLoader: ImageLoader,
): AsyncImagePainter? {
    if (imageUrl == null || decodeSize.width <= 0 || decodeSize.height <= 0) {
        return null
    }
    val platformContext = LocalPlatformContext.current
    val request = remember(platformContext, imageUrl, decodeSize) {
        ImageRequest.Builder(platformContext)
            .data(imageUrl)
            .size(decodeSize.width, decodeSize.height)
            .placeholderMemoryCacheKey(imageUrl)
            .crossfade(false)
            .build()
    }
    return key(imageUrl) {
        rememberAsyncImagePainter(
            model = request,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
        )
    }
}

private fun DrawScope.drawCroppedPainter(
    painter: Painter?,
    alignment: Alignment,
    alpha: Float = 1f,
    alignmentProgress: Float = 1f,
) {
    if (painter == null || alpha <= 0f || size.width <= 0f || size.height <= 0f) {
        return
    }
    val intrinsicSize = painter.intrinsicSize
    val imageSize = if (
        intrinsicSize.isSpecified && intrinsicSize.width.isFinite() && intrinsicSize.width > 0f &&
        intrinsicSize.height.isFinite() && intrinsicSize.height > 0f
    ) {
        intrinsicSize
    } else {
        size
    }
    val scale = ContentScale.Crop.computeScaleFactor(imageSize, size)
    val scaledSize = Size(imageSize.width * scale.scaleX, imageSize.height * scale.scaleY)
    val offset = alignment.align(
        size = IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
        space = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        layoutDirection = layoutDirection,
    )
    val centerOffset = Alignment.Center.align(
        size = IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
        space = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        layoutDirection = layoutDirection,
    )
    clipRect {
        translate(
            left = centerOffset.x + (offset.x - centerOffset.x) * alignmentProgress,
            top = centerOffset.y + (offset.y - centerOffset.y) * alignmentProgress,
        ) {
            with(painter) { draw(size = scaledSize, alpha = alpha) }
        }
    }
}

@Preview
@Composable
private fun StreamCoreSharedArtworkImagePreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreSharedArtworkImage(
            imageUrl = null,
            contentDescription = "Preview artwork",
            fallbackText = "S",
            sharedKey = "preview-artwork",
            clipShape = MaterialTheme.shapes.large,
            modifier = Modifier.width(StreamCoreDimens.Tablet.Browse.LandscapeWidth)
                .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
            scrims = listOf(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.transparentContainer,
                MaterialTheme.colorScheme.scrim,
            ))),
        )
    }
}
