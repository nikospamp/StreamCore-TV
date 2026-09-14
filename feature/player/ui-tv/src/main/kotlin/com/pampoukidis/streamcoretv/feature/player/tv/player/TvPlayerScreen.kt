package com.pampoukidis.streamcoretv.feature.player.tv.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.extensions.onPlayerSurface
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.extensions.playerSurface
import com.pampoukidis.streamcoretv.core.ui.extensions.playerThumbnailPlaceholder
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerControlIcon
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerControlIconType
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerTimelineTrack
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlin.math.roundToLong

@Composable
fun TvPlayerScreen(
    state: PlayerUiState,
    videoSurface: PlaybackVideoSurface?,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val centerFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var previousSettingsPage by remember { mutableStateOf<PlayerSettingsPage?>(null) }
    var controlsWereVisible by remember { mutableStateOf(state.controlsVisible) }
    var playbackFocusInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(state.controlsVisible, state.phase) {
        if (state.settingsPage != null || state.error != null) return@LaunchedEffect
        if (!state.controlsVisible) {
            rootFocusRequester.requestFocus()
        } else if (!controlsWereVisible) {
            playFocusRequester.requestFocus()
        } else if (!playbackFocusInitialized) {
            if (state.phase == PlaybackPhase.Preparing) {
                backFocusRequester.requestFocus()
            } else {
                playFocusRequester.requestFocus()
                playbackFocusInitialized = true
            }
        }
        controlsWereVisible = state.controlsVisible
    }
    LaunchedEffect(state.settingsPage) {
        if (previousSettingsPage != null && state.settingsPage == null) {
            settingsFocusRequester.requestFocus()
        }
        previousSettingsPage = state.settingsPage
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.playerSurface)
            .testTag(PlayerTestTags.Root)
            .focusRequester(rootFocusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                if (!state.controlsVisible) {
                    onAction(PlayerAction.ToggleControls)
                    return@onPreviewKeyEvent true
                }
                onAction(PlayerAction.UserInteraction)
                false
            }
            .focusable(),
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
                    val scale = TvPlayerResizePolicy.scale(
                        mode = state.resizeMode,
                        videoAspectRatio = state.videoAspectRatio,
                        containerAspectRatio = containerAspectRatio,
                    )
                    scaleX = scale
                    scaleY = scale
                },
        )

        if (state.controlsVisible) {
            TvPlayerControls(
                state = state,
                backFocusRequester = backFocusRequester,
                rewindFocusRequester = rewindFocusRequester,
                playFocusRequester = playFocusRequester,
                centerFocusRequester = centerFocusRequester,
                forwardFocusRequester = forwardFocusRequester,
                timelineFocusRequester = timelineFocusRequester,
                settingsFocusRequester = settingsFocusRequester,
                onAction = onAction,
            )
        }

        state.seekFeedbackSeconds?.let { seconds ->
            Text(
                text = if (seconds < 0) "−${-seconds}s" else "+${seconds}s",
                color = MaterialTheme.colorScheme.onPlayerSurface,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(if (seconds < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(StreamCoreDimens.Tv.Screen.HorizontalPadding),
            )
        }

        if (state.error == null) {
            state.settingsPage?.let { page ->
                TvPlayerSettingsOverlay(state = state, page = page, onAction = onAction)
            }
        }
    }

    val error = state.error
    if (error != null) {
        TvPlayerErrorDialog(
            message = error.message,
            isRecoverable = error.isRecoverable,
            onRetry = { onAction(PlayerAction.Retry) },
            onBack = { onAction(PlayerAction.BackSelected) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BoxScope.TvPlayerControls(
    state: PlayerUiState,
    backFocusRequester: FocusRequester,
    rewindFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    centerFocusRequester: FocusRequester,
    forwardFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    onAction: (PlayerAction) -> Unit,
) {
    val isLoading = state.phase == PlaybackPhase.Preparing || state.isBuffering
    val playPauseLabel = when {
        state.isEnded -> "Replay"
        state.isPlaying -> "Pause"
        else -> "Play"
    }
    val playPauseIcon = when {
        state.isEnded -> PlayerControlIconType.Replay
        state.isPlaying -> PlayerControlIconType.Pause
        else -> PlayerControlIconType.Play
    }
    val upperFocusRequester = centerFocusRequester
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.playerSurface.copy(alpha = OverlayAlpha))
            .padding(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tv.Player.OverlayVerticalPadding,
            ),
    ) {
        StreamCoreTvArtworkIconButton(
            contentDescription = "Back",
            onClick = { onAction(PlayerAction.BackSelected) },
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .focusProperties {
                    up = FocusRequester.Cancel
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                    down = centerFocusRequester
                }
                .testTag(PlayerTestTags.Back),
        ) {
            StreamCoreBackIcon(modifier = Modifier.size(StreamCoreDimens.Tv.Player.ControlIconSize))
        }
        Text(
            text = state.title,
            color = MaterialTheme.colorScheme.onPlayerSurface,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
    // Keep the native TV focus target mounted when playback enters or leaves buffering.
    StreamCoreTvArtworkIconButton(
        contentDescription = playPauseLabel,
        onClick = { onAction(PlayerAction.TogglePlayPause) },
        enabled = state.phase != PlaybackPhase.Preparing,
        isLoading = isLoading,
        modifier = Modifier
            .align(Alignment.Center)
            .size(StreamCoreDimens.Tv.Player.LargeControlSize)
            .focusRequester(centerFocusRequester)
            .focusProperties {
                up = backFocusRequester
                down = if (state.canSeek) timelineFocusRequester else playFocusRequester
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .then(if (isLoading) Modifier.testTag(PlayerTestTags.Buffering) else Modifier),
    ) {
        PlayerControlIcon(
            icon = playPauseIcon,
            modifier = Modifier.size(StreamCoreDimens.Tv.Player.LargeControlIconSize),
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.playerSurface.copy(alpha = OverlayAlpha))
            .padding(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tv.Player.OverlayVerticalPadding,
            ),
    ) {
        if (state.isScrubbing) TvPlayerFilmstrip(state)
        val duration = state.durationMillis.coerceAtLeast(1L)
        val position = if (state.isScrubbing) state.scrubPositionMillis else state.positionMillis
        Slider(
            value = position.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { value ->
                if (!state.isScrubbing) onAction(PlayerAction.ScrubStarted)
                onAction(PlayerAction.ScrubChanged(value.roundToLong()))
            },
            onValueChangeFinished = { onAction(PlayerAction.ScrubFinished) },
            enabled = state.canSeek,
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.transparentContainer,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            track = { sliderState ->
                PlayerTimelineTrack(
                    sliderState = sliderState,
                    durationMillis = duration,
                    bufferedPositionMillis = state.bufferedPositionMillis,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    bufferedTrackHeight = StreamCoreDimens.Tv.Player.TimelineBufferedTrackHeight,
                    activeTrackHeight = StreamCoreDimens.Tv.Player.TimelineActiveTrackHeight,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(timelineFocusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            upperFocusRequester.requestFocus()
                            true
                        }
                        Key.DirectionDown -> {
                            playFocusRequester.requestFocus()
                            true
                        }
                        else -> false
                    }
                }
                .focusProperties {
                    up = upperFocusRequester
                    down = playFocusRequester
                }
                .testTag(PlayerTestTags.Timeline),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StreamCoreTvArtworkIconButton(
                contentDescription = "Back 10 seconds",
                onClick = { onAction(PlayerAction.SeekBy(-SeekIntervalMillis, showFeedback = true)) },
                enabled = state.canSeek,
                modifier = Modifier
                    .focusRequester(rewindFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        left = FocusRequester.Cancel
                        right = playFocusRequester
                        down = FocusRequester.Cancel
                    }
                    .testTag(PlayerTestTags.Rewind),
            ) {
                PlayerControlIcon(PlayerControlIconType.Rewind, Modifier.size(StreamCoreDimens.Tv.Player.ControlIconSize))
            }
            StreamCoreTvArtworkIconButton(
                contentDescription = playPauseLabel,
                onClick = { onAction(PlayerAction.TogglePlayPause) },
                enabled = state.phase != PlaybackPhase.Preparing,
                isLoading = isLoading,
                modifier = Modifier
                    .focusRequester(playFocusRequester)
                    .focusProperties {
                        up = if (state.canSeek) timelineFocusRequester else upperFocusRequester
                        left = if (state.canSeek) rewindFocusRequester else FocusRequester.Cancel
                        right = if (state.canSeek) forwardFocusRequester else settingsFocusRequester
                        down = FocusRequester.Cancel
                    }
                    .semantics { stateDescription = state.phase.name }
                    .testTag(PlayerTestTags.PlayPause),
            ) {
                PlayerControlIcon(playPauseIcon, Modifier.size(StreamCoreDimens.Tv.Player.ControlIconSize))
            }
            StreamCoreTvArtworkIconButton(
                contentDescription = "Forward 10 seconds",
                onClick = { onAction(PlayerAction.SeekBy(SeekIntervalMillis, showFeedback = true)) },
                enabled = state.canSeek,
                modifier = Modifier
                    .focusRequester(forwardFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        left = playFocusRequester
                        right = settingsFocusRequester
                        down = FocusRequester.Cancel
                    }
                    .testTag(PlayerTestTags.Forward),
            ) {
                PlayerControlIcon(PlayerControlIconType.Forward, Modifier.size(StreamCoreDimens.Tv.Player.ControlIconSize))
            }
            Text(
                text = formatTime(state.positionMillis) + " / " + formatTime(state.durationMillis),
                color = MaterialTheme.colorScheme.onPlayerSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            StreamCoreTvArtworkIconButton(
                contentDescription = "Playback settings",
                onClick = { onAction(PlayerAction.OpenSettings()) },
                enabled = state.phase != PlaybackPhase.Preparing,
                modifier = Modifier
                    .focusRequester(settingsFocusRequester)
                    .focusProperties {
                        up = if (state.canSeek) timelineFocusRequester else upperFocusRequester
                        left = if (state.canSeek) forwardFocusRequester else playFocusRequester
                        right = FocusRequester.Cancel
                        down = FocusRequester.Cancel
                    }
                    .testTag(PlayerTestTags.SettingsButton),
            ) {
                PlayerControlIcon(PlayerControlIconType.Settings, Modifier.size(StreamCoreDimens.Tv.Player.ControlIconSize))
            }
        }
    }
}

@Composable
private fun TvPlayerFilmstrip(state: PlayerUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PlayerTestTags.Filmstrip),
    ) {
        Text(
            text = formatTime(state.scrubPositionMillis),
            color = MaterialTheme.colorScheme.onPlayerSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            state.filmstripFrames.forEachIndexed { index, frame ->
                val isCenter = index == state.filmstripFrames.lastIndex / 2
                Box(
                    modifier = Modifier
                        .size(
                            width = if (isCenter) {
                                StreamCoreDimens.Tv.Player.FilmstripFocusedFrameWidth
                            } else {
                                StreamCoreDimens.Tv.Player.FilmstripFrameWidth
                            },
                            height = if (isCenter) {
                                StreamCoreDimens.Tv.Player.FilmstripFocusedFrameHeight
                            } else {
                                StreamCoreDimens.Tv.Player.FilmstripFrameHeight
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
private fun TvPlayerErrorDialog(
    message: String,
    isRecoverable: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isRecoverable) {
        if (isRecoverable) {
            retryFocusRequester.requestFocus()
        } else {
            backFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .width(StreamCoreDimens.Tv.Player.ErrorMaxWidth)
                    .testTag(PlayerTestTags.Error),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                    modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge),
                ) {
                    Text(text = "Playback unavailable", style = MaterialTheme.typography.headlineSmall)
                    Text(text = message, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
                        StreamCoreTvButton(
                            text = "Back",
                            onClick = onBack,
                            enabled = true,
                            variant = StreamCoreTvButtonVariant.Tertiary,
                            modifier = Modifier
                                .focusRequester(backFocusRequester)
                                .focusProperties {
                                    left = FocusRequester.Cancel
                                    right = if (isRecoverable) retryFocusRequester else FocusRequester.Cancel
                                },
                        )
                        if (isRecoverable) {
                            StreamCoreTvButton(
                                text = "Retry",
                                onClick = onRetry,
                                enabled = true,
                                variant = StreamCoreTvButtonVariant.Primary,
                                modifier = Modifier
                                    .focusRequester(retryFocusRequester)
                                    .focusProperties {
                                        left = backFocusRequester
                                        right = FocusRequester.Cancel
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private const val SeekIntervalMillis = 10_000L
private const val OverlayAlpha = 0.68f

private val previewTracks = listOf(
    PlaybackTrackModel("video-1080", PlaybackTrackType.Video, "1080p"),
    PlaybackTrackModel("video-720", PlaybackTrackType.Video, "720p"),
)

@PreviewTV
@Composable
private fun TvPlayerPreparingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerScreen(
            state = PlayerUiState(title = "Orbit Fall", phase = PlaybackPhase.Preparing),
            videoSurface = null,
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvPlayerPlayingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerScreen(
            state = previewState(isPlaying = true),
            videoSurface = null,
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvPlayerPausedPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerScreen(
            state = previewState(isPlaying = false),
            videoSurface = null,
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvPlayerBufferingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerScreen(
            state = previewState(isPlaying = true).copy(phase = PlaybackPhase.Buffering),
            videoSurface = null,
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvPlayerSettingsPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerScreen(
            state = previewState(isPlaying = false).copy(settingsPage = PlayerSettingsPage.Root),
            videoSurface = null,
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvPlayerErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerScreen(
            state = previewState(isPlaying = false).copy(
                phase = PlaybackPhase.Error,
                error = PlaybackErrorModel(
                    code = "PREVIEW",
                    message = "Unable to load this video.",
                    isRecoverable = true,
                ),
            ),
            videoSurface = null,
            onAction = {},
        )
    }
}

private fun previewState(isPlaying: Boolean): PlayerUiState {
    return PlayerUiState(
        title = "Orbit Fall",
        phase = PlaybackPhase.Ready,
        isPlaying = isPlaying,
        positionMillis = 420_000L,
        durationMillis = 5_400_000L,
        bufferedPositionMillis = 960_000L,
        videoTracks = previewTracks,
        selectedVideoTrackId = previewTracks.first().id,
    )
}
