package com.pampoukidis.streamcoretv.feature.player.mobile.player

import com.pampoukidis.streamcoretv.core.tracing.benchmarkReadiness

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.onPlayerSurface
import com.pampoukidis.streamcoretv.core.ui.extensions.playerSurface
import com.pampoukidis.streamcoretv.core.ui.extensions.playerThumbnailPlaceholder
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.mobile.R
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlin.math.roundToLong

@Composable
fun MobilePlayerScreen(
    state: PlayerUiState,
    videoSurface: PlaybackVideoSurface?,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animationMillis = remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        if (scale == 0f) 0 else ControlsAnimationMillis
    }

    Box(
        modifier = modifier
            .testTag(PlayerTestTags.Root)
            .benchmarkReadiness("player", state.phase == PlaybackPhase.Ready && state.isPlaying)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.playerSurface)
            .pointerInput(state.durationMillis) {
                detectTapGestures(
                    onTap = { onAction(PlayerAction.ToggleControls) },
                    onDoubleTap = { offset ->
                        val delta = if (offset.x < size.width / 2f) -SeekIntervalMillis else SeekIntervalMillis
                        onAction(PlayerAction.SeekBy(delta, showFeedback = true))
                    },
                )
            },
    ) {
        videoSurface?.Render(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val containerAspectRatio = if (size.height > 0f) {
                        size.width / size.height
                    } else {
                        0f
                    }
                    val fillScale = MobilePlayerResizePolicy.scale(
                        mode = state.resizeMode,
                        videoAspectRatio = state.videoAspectRatio,
                        containerAspectRatio = containerAspectRatio,
                    )
                    scaleX = fillScale
                    scaleY = fillScale
                },
        )

        if (!state.isInPip) {
            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(tween(animationMillis)) + slideInVertically(tween(animationMillis)) { -it / 8 },
                exit = fadeOut(tween(animationMillis)) + slideOutVertically(tween(animationMillis)) { -it / 8 },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                PlayerTopBar(state.title, onAction)
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(tween(animationMillis)),
                exit = fadeOut(tween(animationMillis)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                PlayerCenterControl(state, onAction)
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(tween(animationMillis)) + slideInVertically(tween(animationMillis)) { it / 6 },
                exit = fadeOut(tween(animationMillis)) + slideOutVertically(tween(animationMillis)) { it / 6 },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                PlayerBottomControls(state, onAction)
            }

            state.seekFeedbackSeconds?.let { seconds ->
                Text(
                    text = if (seconds < 0) "−${-seconds}s" else "+${seconds}s",
                    color = MaterialTheme.colorScheme.onPlayerSurface,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .align(if (seconds < 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = StreamCoreDimens.Mobile.Player.SeekFeedbackHorizontalOffset)
                        .background(
                            MaterialTheme.colorScheme.playerSurface.copy(alpha = 0.72f),
                            MaterialTheme.shapes.medium,
                        )
                        .padding(
                            horizontal = StreamCoreDimens.Spacing.Large,
                            vertical = StreamCoreDimens.Mobile.Player.OverlayVerticalPadding,
                        ),
                )
            }

            PlayerSettingsOverlay(
                state = state,
                page = state.settingsPage,
                onAction = onAction,
            )

            state.error?.let { error ->
                PlayerError(error.message, onAction)
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    onAction: (PlayerAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.playerSurface.copy(alpha = OverlayAlpha))
            .padding(
                horizontal = StreamCoreDimens.Mobile.Player.OverlayHorizontalPadding,
                vertical = StreamCoreDimens.Mobile.Player.OverlayVerticalPadding,
            ),
    ) {
        StreamCoreIconButton(
            contentDescription = "Back",
            onClick = { onAction(PlayerAction.BackSelected) },
        ) { StreamCoreBackIcon() }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPlayerSurface,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlayerCenterControl(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    when {
        state.isBuffering || state.phase == PlaybackPhase.Preparing -> CircularProgressIndicator(
            modifier = Modifier.testTag(PlayerTestTags.Buffering),
        )

        state.isEnded -> PlayerIconButton(
            icon = R.drawable.ic_player_replay_24,
            description = "Replay",
            onClick = { onAction(PlayerAction.TogglePlayPause) },
            large = true,
        )

        else -> PlayerIconButton(
            icon = if (state.isPlaying) R.drawable.ic_player_pause_24 else null,
            description = if (state.isPlaying) "Pause" else "Play",
            onClick = { onAction(PlayerAction.TogglePlayPause) },
            large = true,
        )
    }
}

@Composable
private fun PlayerBottomControls(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.playerSurface.copy(alpha = OverlayAlpha))
            .navigationBarsPadding()
            .padding(
                horizontal = StreamCoreDimens.Mobile.Player.OverlayHorizontalPadding,
                vertical = StreamCoreDimens.Mobile.Player.OverlayVerticalPadding,
            ),
    ) {
        if (state.isScrubbing) {
            Filmstrip(state)
        }
        BufferedTimeline(state, onAction)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlayerIconButton(
                icon = R.drawable.ic_replay_10_24dp,
                description = "Back 10 seconds",
                enabled = state.canSeek,
                onClick = { onAction(PlayerAction.SeekBy(-SeekIntervalMillis)) },
            )
            PlayerIconButton(
                icon = if (state.isPlaying) R.drawable.ic_player_pause_24 else null,
                description = if (state.isPlaying) "Pause" else "Play",
                onClick = { onAction(PlayerAction.TogglePlayPause) },
            )
            PlayerIconButton(
                icon = R.drawable.ic_forward_10_24dp,
                description = "Forward 10 seconds",
                enabled = state.canSeek,
                onClick = { onAction(PlayerAction.SeekBy(SeekIntervalMillis)) },
            )
            Text(
                text = "${formatTime(state.positionMillis)} / ${formatTime(state.durationMillis)}",
                color = MaterialTheme.colorScheme.onPlayerSurface,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.weight(1f))
            PlayerIconButton(
                icon = R.drawable.ic_player_settings_24,
                description = "Playback settings",
                onClick = { onAction(PlayerAction.OpenSettings()) },
            )
            if (state.isPipSupported) {
                PlayerIconButton(
                    icon = R.drawable.ic_player_pip_24,
                    description = "Picture in picture",
                    onClick = { onAction(PlayerAction.PipSelected) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BufferedTimeline(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    val duration = state.durationMillis.coerceAtLeast(1L)
    val displayedPosition = if (state.isScrubbing) state.scrubPositionMillis else state.positionMillis
    val sliderColors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.transparentContainer,
        thumbColor = MaterialTheme.colorScheme.primary,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.testTag(PlayerTestTags.Timeline),
    ) {
        Slider(
            value = displayedPosition.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { value ->
                if (!state.isScrubbing) onAction(PlayerAction.ScrubStarted)
                onAction(PlayerAction.ScrubChanged(value.roundToLong()))
            },
            onValueChangeFinished = { onAction(PlayerAction.ScrubFinished) },
            enabled = state.canSeek,
            valueRange = 0f..duration.toFloat(),
            colors = sliderColors,
            track = { sliderState ->
                PlayerTimelineTrack(
                    sliderState = sliderState,
                    durationMillis = duration,
                    bufferedPositionMillis = state.bufferedPositionMillis,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
private fun PlayerTimelineTrack(
    sliderState: SliderState,
    durationMillis: Long,
    bufferedPositionMillis: Long,
    activeTrackColor: Color,
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
                .height(StreamCoreDimens.Mobile.Player.TimelineBufferedTrackHeight),
        )

        // Foreground layer: played progress, kept at the Material 3 active-track height.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(StreamCoreDimens.Mobile.Player.TimelineActiveTrackHeight),
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

@Composable
private fun Filmstrip(state: PlayerUiState) {
    val frameCount = if (LocalConfiguration.current.screenWidthDp < ConstrainedWidthDp) 3 else 5
    val frames = if (frameCount == 3) {
        state.filmstripFrames.drop(1).take(3)
    } else {
        state.filmstripFrames.take(frameCount)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PlayerTestTags.Filmstrip),
    ) {
        Text(
            text = formatTime(state.scrubPositionMillis),
            color = MaterialTheme.colorScheme.onPlayerSurface,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
            frames.forEachIndexed { index, frame ->
                val isCenter = index == frames.lastIndex / 2
                Box(
                    modifier = Modifier
                        .size(
                            width = if (isCenter) {
                                StreamCoreDimens.Mobile.Player.FilmstripFocusedFrameWidth
                            } else {
                                StreamCoreDimens.Mobile.Player.FilmstripFrameWidth
                            },
                            height = if (isCenter) {
                                StreamCoreDimens.Mobile.Player.FilmstripFocusedFrameHeight
                            } else {
                                StreamCoreDimens.Mobile.Player.FilmstripFrameHeight
                            },
                        )
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.playerThumbnailPlaceholder),
                ) {
                    frame.image?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PlayerError(message: String, onAction: (PlayerAction) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.playerSurface.copy(alpha = 0.9f),
        modifier = Modifier
            .testTag(PlayerTestTags.Error)
            .align(Alignment.Center)
            .widthIn(max = StreamCoreDimens.Mobile.Player.ErrorMaxWidth),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            Text(
                "Playback unavailable",
                color = MaterialTheme.colorScheme.onPlayerSurface,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(message, color = MaterialTheme.colorScheme.onPlayerSurface.copy(alpha = 0.8f))
            Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
                StreamCoreButton("Back", { onAction(PlayerAction.BackSelected) }, true)
                StreamCoreButton("Retry", { onAction(PlayerAction.Retry) }, true)
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: Int?,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    large: Boolean = false,
) {
    StreamCoreIconButton(
        contentDescription = description,
        onClick = onClick,
        enabled = enabled,
        modifier = if (large) Modifier.size(StreamCoreDimens.Mobile.Player.LargeControlSize) else Modifier,
    ) {
        if (icon == null) {
            StreamCorePlayIcon(
                Modifier.size(
                    if (large) {
                        StreamCoreDimens.Mobile.Player.LargeControlIconSize
                    } else {
                        StreamCoreDimens.Icon.Standard
                    },
                ),
            )
        } else {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(
                    if (large) {
                        StreamCoreDimens.Mobile.Player.LargeControlIconSize
                    } else {
                        StreamCoreDimens.Icon.Standard
                    },
                ),
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private const val ControlsAnimationMillis = 200
private const val OverlayAlpha = 0.68f
private const val SeekIntervalMillis = 10_000L
private const val ConstrainedWidthDp = 600

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun MobilePlayerScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobilePlayerScreen(
            state = PlayerUiState(
                title = "Sintel",
                phase = PlaybackPhase.Ready,
                durationMillis = 888_000L,
                positionMillis = 321_000L,
                bufferedPositionMillis = 480_000L,
            ),
            videoSurface = PreviewVideoSurface,
            onAction = {},
        )
    }
}

private object PreviewVideoSurface : PlaybackVideoSurface {
    @Composable
    override fun Render(modifier: Modifier) {
        Box(modifier.background(MaterialTheme.colorScheme.playerSurface))
    }
}
