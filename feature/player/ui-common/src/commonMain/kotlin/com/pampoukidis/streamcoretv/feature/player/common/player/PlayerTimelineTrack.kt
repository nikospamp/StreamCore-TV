package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.extensions.onPlayerSurface
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/**
 * Draws the buffered and played layers inside the [Slider]'s track coordinate space.
 *
 * The configurable Material 3 [SliderDefaults.Track] omits the active segment while its width is
 * less than or equal to the track's outside corner radius. With our 16 dp active track, that creates
 * an 8 dp dead zone which can represent several seconds of a long video. This custom active layer
 * shrinks only that outside radius until the segment reaches 8 dp, then matches the Material shape.
 *
 * The [Slider] still owns gestures, semantics, value clamping, and thumb rendering. This composable
 * only draws the thin buffered track and the thick orange played segment within the exact bounds
 * provided by the slider's `track` slot.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlayerTimelineTrack(
    sliderState: SliderState,
    durationMillis: Long,
    bufferedPositionMillis: Long,
    activeTrackColor: Color,
    bufferedTrackHeight: Dp,
    activeTrackHeight: Dp,
) {
    // Canvas redraws as SliderState changes; reuse the mutable path to avoid allocating one per tick.
    val activeTrackPath = remember { Path() }
    Box(contentAlignment = Alignment.Center) {
        // Base layer: unbuffered track plus the lighter buffered range.
        LinearProgressIndicator(
            progress = {
                (bufferedPositionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
            },
            color = MaterialTheme.colorScheme.onPlayerSurface.copy(alpha = 0.5f),
            trackColor = MaterialTheme.colorScheme.onPlayerSurface.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(bufferedTrackHeight),
        )

        // Foreground layer: played progress, kept at the Material 3 active-track height.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(activeTrackHeight),
        ) {
            // Map the current slider value to this track's physical width.
            val playedFraction = (sliderState.value / durationMillis).coerceIn(0f, 1f)
            val activeTrackWidth = size.width * playedFraction
            if (activeTrackWidth <= 0f) {
                return@Canvas
            }

            // Shrink the outside radius for early progress; cap it at the normal 8 dp radius later.
            val outsideCornerRadius = activeTrackWidth.coerceAtMost(size.height / 2f)
            val outsideCorner = CornerRadius(outsideCornerRadius, outsideCornerRadius)
            val insideCorner = CornerRadius(0f, 0f)

            // Anchor progress to the logical start: left in LTR, right in RTL.
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val bounds = if (isRtl) {
                Rect(
                    left = size.width - activeTrackWidth,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                )
            } else {
                Rect(
                    left = 0f,
                    top = 0f,
                    right = activeTrackWidth,
                    bottom = size.height,
                )
            }

            // Round only the outer edge. The thumb-facing edge stays square beneath the thumb.
            val track = if (isRtl) {
                RoundRect(
                    rect = bounds,
                    topLeft = insideCorner,
                    topRight = outsideCorner,
                    bottomRight = outsideCorner,
                    bottomLeft = insideCorner,
                )
            } else {
                RoundRect(
                    rect = bounds,
                    topLeft = outsideCorner,
                    topRight = insideCorner,
                    bottomRight = insideCorner,
                    bottomLeft = outsideCorner,
                )
            }

            // Path is cleared after drawing so the remembered instance is ready for the next frame.
            activeTrackPath.addRoundRect(track)
            drawPath(activeTrackPath, activeTrackColor)
            activeTrackPath.rewind()
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerTimelineTrackPreview() {
    StreamCoreTheme(darkTheme = true) {
        PlayerTimelineTrack(
            sliderState = remember { SliderState(value = 300f, valueRange = 0f..1_000f) },
            durationMillis = 1_000L,
            bufferedPositionMillis = 600L,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            bufferedTrackHeight = StreamCoreDimens.Mobile.Player.TimelineBufferedTrackHeight,
            activeTrackHeight = StreamCoreDimens.Mobile.Player.TimelineActiveTrackHeight,
        )
    }
}
