package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerControlIcon
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerControlIconType
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerTimelineTrack
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerFixtures
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerShowcaseScenario
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.player.ui_web.generated.resources.Res
import streamcoretv.feature.player.ui_web.generated.resources.web_player_activation_message
import streamcoretv.feature.player.ui_web.generated.resources.web_player_activation_title
import streamcoretv.feature.player.ui_web.generated.resources.web_player_back
import streamcoretv.feature.player.ui_web.generated.resources.web_player_buffering
import streamcoretv.feature.player.ui_web.generated.resources.web_player_ended
import streamcoretv.feature.player.ui_web.generated.resources.web_player_forward
import streamcoretv.feature.player.ui_web.generated.resources.web_player_fullscreen_enter
import streamcoretv.feature.player.ui_web.generated.resources.web_player_fullscreen_exit
import streamcoretv.feature.player.ui_web.generated.resources.web_player_pause
import streamcoretv.feature.player.ui_web.generated.resources.web_player_play
import streamcoretv.feature.player.ui_web.generated.resources.web_player_preparing
import streamcoretv.feature.player.ui_web.generated.resources.web_player_preview_frame
import streamcoretv.feature.player.ui_web.generated.resources.web_player_preview_unavailable
import streamcoretv.feature.player.ui_web.generated.resources.web_player_replay
import streamcoretv.feature.player.ui_web.generated.resources.web_player_rewind
import streamcoretv.feature.player.ui_web.generated.resources.web_player_seek_feedback_back
import streamcoretv.feature.player.ui_web.generated.resources.web_player_seek_feedback_forward
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings
import streamcoretv.feature.player.ui_web.generated.resources.web_player_timeline
import streamcoretv.feature.player.ui_web.generated.resources.web_player_title_fallback

@Composable
fun WebPlayerScreen(
    state: PlayerUiState,
    videoSurface: PlaybackVideoSurface?,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnAction by rememberUpdatedState(onAction)
    val overlayVisible = state.settingsPage != null || state.error != null
    val fullscreenController = LocalWebPlayerFullscreenController.current
    val isFullscreen by fullscreenController.isFullscreen
    val rootFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val fullscreenFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var previousControlsVisible by remember { mutableStateOf(state.controlsVisible) }
    var previousSettingsPage by remember { mutableStateOf<PlayerSettingsPage?>(state.settingsPage) }
    var previousFullscreen by remember { mutableStateOf(isFullscreen) }
    var initialFocusAssigned by remember { mutableStateOf(false) }
    var previousErrorVisible by remember { mutableStateOf(state.error != null) }

    WebPlayerDocumentEscapeEffect {
        if (overlayVisible) {
            currentOnAction(PlayerAction.BackSelected)
        } else if (isFullscreen) {
            fullscreenController.exit()
        } else {
            currentOnAction(PlayerAction.BackSelected)
        }
    }
    WebPlayerDocumentControlsRevealEffect(
        enabled = !state.controlsVisible && state.settingsPage == null && state.error == null,
        onReveal = { currentOnAction(PlayerAction.UserInteraction) },
    )

    LaunchedEffect(state.controlsVisible, state.phase, state.error, state.settingsPage) {
        if (state.error != null || state.settingsPage != null) {
            return@LaunchedEffect
        }
        if (previousSettingsPage != null) {
            previousControlsVisible = state.controlsVisible
            return@LaunchedEffect
        }
        when {
            !state.controlsVisible -> rootFocusRequester.requestFocusWhenReady()
            !previousControlsVisible -> playFocusRequester.requestFocusWhenReady()
            !initialFocusAssigned && state.phase == PlaybackPhase.Preparing -> {
                backFocusRequester.requestFocusWhenReady()
            }

            !initialFocusAssigned -> {
                playFocusRequester.requestFocusWhenReady()
                initialFocusAssigned = true
            }
        }
        previousControlsVisible = state.controlsVisible
    }

    LaunchedEffect(state.settingsPage) {
        if (previousSettingsPage != null && state.settingsPage == null) {
            settingsFocusRequester.requestFocusWhenReady()
        }
        previousSettingsPage = state.settingsPage
    }

    LaunchedEffect(state.error) {
        if (previousErrorVisible && state.error == null) {
            val target = when {
                !state.controlsVisible -> rootFocusRequester
                state.phase == PlaybackPhase.Preparing -> backFocusRequester
                else -> playFocusRequester
            }
            target.requestFocusWhenReady()
        }
        previousErrorVisible = state.error != null
    }

    LaunchedEffect(isFullscreen) {
        if (previousFullscreen && !isFullscreen && state.settingsPage == null && state.error == null) {
            fullscreenFocusRequester.requestFocusWhenReady()
        }
        previousFullscreen = isFullscreen
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PlayerTestTags.Root)
            .focusRequester(rootFocusRequester)
            .onPreviewKeyEvent { event ->
                handleRootKeyEvent(
                    event = event,
                    controlsVisible = state.controlsVisible || overlayVisible,
                    isFullscreen = isFullscreen && !overlayVisible,
                    onExitFullscreen = fullscreenController::exit,
                    onAction = currentOnAction,
                )
            }
            .focusable(enabled = !overlayVisible),
    ) {
        WebPlayerVideoSurface(
            state = state,
            videoSurface = videoSurface,
        )

        val underlyingControlsModifier = if (overlayVisible) {
            Modifier.clearAndSetSemantics { }
                .focusProperties { onEnter = { cancelFocusChange() } }
                .focusGroup()
        } else {
            Modifier
        }
        Box(modifier = Modifier.fillMaxSize().then(underlyingControlsModifier)) {
            WebPlayerInteractionOverlay(
                controlsVisible = state.controlsVisible,
                onAction = currentOnAction,
            )

            if (state.controlsVisible) {
                WebPlayerControls(
                    state = state,
                    isFullscreen = isFullscreen,
                    backFocusRequester = backFocusRequester,
                    rewindFocusRequester = rewindFocusRequester,
                    playFocusRequester = playFocusRequester,
                    forwardFocusRequester = forwardFocusRequester,
                    timelineFocusRequester = timelineFocusRequester,
                    fullscreenFocusRequester = fullscreenFocusRequester,
                    settingsFocusRequester = settingsFocusRequester,
                    onBack = {
                        if (isFullscreen) {
                            fullscreenController.exit()
                        } else {
                            currentOnAction(PlayerAction.BackSelected)
                        }
                    },
                    onFullscreen = fullscreenController::toggle,
                    onAction = currentOnAction,
                )
            }

            if (state.error == null) {
                WebPlayerStatus(state = state)
            }

            state.seekFeedbackSeconds?.let { seconds ->
                WebPlayerSeekFeedback(seconds = seconds)
            }
        }
        val error = state.error
        if (error != null) {
            WebPlayerErrorOverlay(
                message = error.message,
                recoverable = error.isRecoverable,
                onAction = currentOnAction,
            )
        } else {
            state.settingsPage?.let { page ->
                WebPlayerSettingsOverlay(
                    state = state,
                    page = page,
                    onAction = currentOnAction,
                )
            }
        }
    }
}

@Composable
private fun WebPlayerVideoSurface(
    state: PlayerUiState,
    videoSurface: PlaybackVideoSurface?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(WebPlayerZOrder.VideoSurface)
            .testTag(WebPlayerTestTags.VideoSurface),
    ) {
        if (videoSurface == null) {
            Text(
                text = state.title.ifBlank {
                    stringResource(Res.string.web_player_title_fallback)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = WebPlayerTokens.PlaceholderAlpha,
                ),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = WebPlayerTokens.StatusPanelMaxWidth)
                    .padding(StreamCoreDimens.Spacing.ExtraLarge),
            )
        } else {
            videoSurface.Render(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun WebPlayerInteractionOverlay(
    controlsVisible: Boolean,
    onAction: (PlayerAction) -> Unit,
) {
    val currentOnAction by rememberUpdatedState(onAction)
    val pointerModifier = if (controlsVisible) {
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                currentOnAction(PlayerAction.ToggleControls)
            }
        }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(WebPlayerZOrder.Interaction)
            .then(pointerModifier),
    )
}

@Composable
private fun BoxScope.WebPlayerControls(
    state: PlayerUiState,
    isFullscreen: Boolean,
    backFocusRequester: FocusRequester,
    rewindFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    forwardFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    fullscreenFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onFullscreen: () -> Unit,
    onAction: (PlayerAction) -> Unit,
) {
    val centerFocusRequester = remember { FocusRequester() }
    val scrimColor = MaterialTheme.colorScheme.scrim
    val topBrush = remember(scrimColor) {
        Brush.verticalGradient(listOf(scrimColor.copy(alpha = WebPlayerTokens.SurfaceScrimAlpha), scrimColor.copy(alpha = 0f)))
    }
    val bottomBrush = remember(scrimColor) {
        Brush.verticalGradient(listOf(scrimColor.copy(alpha = 0f), scrimColor.copy(alpha = WebPlayerTokens.SurfaceScrimAlpha)))
    }
    val playPauseLabel = when {
        state.isEnded -> stringResource(Res.string.web_player_replay)
        state.isPlaying -> stringResource(Res.string.web_player_pause)
        else -> stringResource(Res.string.web_player_play)
    }
    val playPauseIcon = when {
        state.isEnded -> PlayerControlIconType.Replay
        state.isPlaying -> PlayerControlIconType.Pause
        else -> PlayerControlIconType.Play
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(WebPlayerZOrder.Controls)
            .fillMaxWidth().background(topBrush)
            .padding(horizontal = WebPlayerTokens.ScreenHorizontalPadding, vertical = WebPlayerTokens.ScreenVerticalPadding)
            .testTag(WebPlayerTestTags.Controls),
    ) {
        StreamCoreWebArtworkIconButton(
            contentDescription = stringResource(Res.string.web_player_back),
            onClick = onBack,
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .webPlayerDirectionalFocus(down = playFocusRequester)
                .focusProperties {
                    right = FocusRequester.Cancel
                    down = playFocusRequester
                }
                .testTag(PlayerTestTags.Back),
        ) {
            StreamCoreBackIcon(modifier = Modifier.size(WebPlayerTokens.ControlIconSize))
        }
        Text(
            text = state.title.ifBlank { stringResource(Res.string.web_player_title_fallback) },
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f),
        )
    }
    StreamCoreWebArtworkIconButton(
        contentDescription = playPauseLabel,
        onClick = { onAction(PlayerAction.TogglePlayPause) },
        enabled = state.phase != PlaybackPhase.Preparing,
        isLoading = state.phase == PlaybackPhase.Preparing || state.isBuffering,
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(WebPlayerZOrder.Controls)
            .size(WebPlayerTokens.LargeControlSize).focusRequester(centerFocusRequester)
            .webPlayerDirectionalFocus(up = backFocusRequester, down = timelineFocusRequester)
            .focusProperties {
                up = backFocusRequester
                down = timelineFocusRequester
            },
    ) {
        PlayerControlIcon(playPauseIcon, Modifier.size(WebPlayerTokens.LargeControlIconSize))
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(WebPlayerZOrder.Controls)
            .fillMaxWidth().background(bottomBrush)
            .padding(horizontal = WebPlayerTokens.ScreenHorizontalPadding, vertical = WebPlayerTokens.ScreenVerticalPadding),
    ) {
        if (state.isScrubbing) WebPlayerFilmstrip(state)
        WebPlayerTimeline(
            state = state,
            focusRequester = timelineFocusRequester,
            playFocusRequester = playFocusRequester,
            settingsFocusRequester = settingsFocusRequester,
            onAction = onAction,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            StreamCoreWebArtworkIconButton(
                contentDescription = stringResource(Res.string.web_player_rewind),
                onClick = { onAction(PlayerAction.SeekBy(-WebPlayerTokens.SeekIntervalMillis, showFeedback = true)) },
                enabled = state.canSeek,
                modifier = Modifier
                    .focusRequester(rewindFocusRequester)
                    .webPlayerDirectionalFocus(up = timelineFocusRequester, right = playFocusRequester, down = timelineFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        left = FocusRequester.Cancel
                        right = playFocusRequester
                        down = timelineFocusRequester
                    }
                    .testTag(PlayerTestTags.Rewind),
            ) {
                PlayerControlIcon(PlayerControlIconType.Rewind, Modifier.size(WebPlayerTokens.ControlIconSize))
            }
            StreamCoreWebArtworkIconButton(
                contentDescription = playPauseLabel,
                onClick = { onAction(PlayerAction.TogglePlayPause) },
                enabled = state.phase != PlaybackPhase.Preparing,
                isLoading = state.phase == PlaybackPhase.Preparing || state.isBuffering,
                modifier = Modifier
                    .focusRequester(playFocusRequester)
                    .webPlayerDirectionalFocus(up = backFocusRequester, left = rewindFocusRequester, right = forwardFocusRequester, down = timelineFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                        left = rewindFocusRequester
                        right = forwardFocusRequester
                        down = timelineFocusRequester
                    }
                    .semantics { stateDescription = state.phase.name }
                    .testTag(PlayerTestTags.PlayPause),
            ) {
                PlayerControlIcon(playPauseIcon, Modifier.size(WebPlayerTokens.ControlIconSize))
            }
            StreamCoreWebArtworkIconButton(
                contentDescription = stringResource(Res.string.web_player_forward),
                onClick = { onAction(PlayerAction.SeekBy(WebPlayerTokens.SeekIntervalMillis, showFeedback = true)) },
                enabled = state.canSeek,
                modifier = Modifier
                    .focusRequester(forwardFocusRequester)
                    .webPlayerDirectionalFocus(up = timelineFocusRequester, left = playFocusRequester, right = fullscreenFocusRequester, down = timelineFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        left = playFocusRequester
                        right = fullscreenFocusRequester
                        down = timelineFocusRequester
                    }
                    .testTag(PlayerTestTags.Forward),
            ) {
                PlayerControlIcon(PlayerControlIconType.Forward, Modifier.size(WebPlayerTokens.ControlIconSize))
            }
            Text(
                text = "${formatPlaybackTime(state.displayPositionMillis())} / " + formatPlaybackTime(state.durationMillis),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            StreamCoreWebArtworkIconButton(
                contentDescription = stringResource(if (isFullscreen) Res.string.web_player_fullscreen_exit else Res.string.web_player_fullscreen_enter),
                onClick = onFullscreen,
                modifier = Modifier
                    .focusRequester(fullscreenFocusRequester)
                    .webPlayerDirectionalFocus(up = timelineFocusRequester, left = forwardFocusRequester, right = settingsFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        left = forwardFocusRequester
                        right = settingsFocusRequester
                        down = FocusRequester.Cancel
                    }
                    .testTag(WebPlayerTestTags.Fullscreen),
            ) {
                PlayerControlIcon(if (isFullscreen) PlayerControlIconType.FullscreenExit else PlayerControlIconType.Fullscreen, Modifier.size(WebPlayerTokens.ControlIconSize))
            }
            StreamCoreWebArtworkIconButton(
                contentDescription = stringResource(Res.string.web_player_settings),
                onClick = { onAction(PlayerAction.OpenSettings()) },
                enabled = state.phase != PlaybackPhase.Preparing,
                modifier = Modifier
                    .focusRequester(settingsFocusRequester)
                    .webPlayerDirectionalFocus(up = timelineFocusRequester, left = fullscreenFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        left = fullscreenFocusRequester
                        right = FocusRequester.Cancel
                        down = FocusRequester.Cancel
                    }
                    .testTag(PlayerTestTags.SettingsButton),
            ) {
                PlayerControlIcon(PlayerControlIconType.Settings, Modifier.size(WebPlayerTokens.ControlIconSize))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WebPlayerTimeline(
    state: PlayerUiState,
    focusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    onAction: (PlayerAction) -> Unit,
) {
    val duration = state.durationMillis.coerceAtLeast(1L)
    val position = state.displayPositionMillis().coerceIn(0L, duration)
    val positionText = formatPlaybackTime(position)
    val durationText = formatPlaybackTime(state.durationMillis)
    val timelineDescription = stringResource(
        Res.string.web_player_timeline,
        positionText,
        durationText,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WebPlayerTokens.TimelineHeight)
            .onPreviewKeyEvent { event ->
                handleTimelineKeyEvent(
                    event = event,
                    state = state,
                    playFocusRequester = playFocusRequester,
                    settingsFocusRequester = settingsFocusRequester,
                    onAction = onAction,
                )
            },
    ) {
        Slider(
            value = position.toFloat(),
            onValueChange = { value ->
                if (!state.isScrubbing) {
                    onAction(PlayerAction.ScrubStarted)
                }
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
                    bufferedTrackHeight = StreamCoreDimens.Spacing.Tiny,
                    activeTrackHeight = StreamCoreDimens.Spacing.Large,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusProperties {
                    up = playFocusRequester
                    down = settingsFocusRequester
                }
                .semantics {
                    contentDescription = timelineDescription
                }
                .testTag(PlayerTestTags.Timeline),
        )
    }
}

@Composable
private fun WebPlayerFilmstrip(state: PlayerUiState) {
    val frameItems = remember(state.filmstripFrames) {
        state.filmstripFrames.mapIndexed { index, frame ->
            WebPlayerFilmstripItem(
                key = "$index:${frame.positionMillis}",
                frame = frame,
                centered = index == state.filmstripFrames.lastIndex / 2,
            )
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PlayerTestTags.Filmstrip),
    ) {
        Text(
            text = formatPlaybackTime(state.scrubPositionMillis),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )
        if (frameItems.none { item -> item.frame.image != null }) {
            Text(
                text = stringResource(Res.string.web_player_preview_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .testTag(WebPlayerTestTags.NoFilmstrip),
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            ) {
                items(
                    items = frameItems,
                    key = { item -> item.key },
                    contentType = { item -> if (item.centered) "center-frame" else "frame" },
                ) { item ->
                    val previewDescription = stringResource(
                        Res.string.web_player_preview_frame,
                        formatPlaybackTime(item.frame.positionMillis),
                    )
                    val width = if (item.centered) {
                        WebPlayerTokens.FilmstripFocusedFrameWidth
                    } else {
                        WebPlayerTokens.FilmstripFrameWidth
                    }
                    val height = if (item.centered) {
                        WebPlayerTokens.FilmstripFocusedFrameHeight
                    } else {
                        WebPlayerTokens.FilmstripFrameHeight
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = width, height = height)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .semantics {
                                contentDescription = previewDescription
                            },
                    ) {
                        val image = item.frame.image
                        if (image != null) {
                            androidx.compose.foundation.Image(
                                bitmap = image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.WebPlayerStatus(state: PlayerUiState) {
    when {
        (state.phase == PlaybackPhase.Idle || state.phase == PlaybackPhase.Preparing) && !state.controlsVisible -> {
            WebPlayerProgressStatus(
                text = stringResource(Res.string.web_player_preparing),
                testTag = WebPlayerTestTags.Preparing,
            )
        }

        state.isBuffering && !state.controlsVisible -> {
            WebPlayerProgressStatus(
                text = stringResource(Res.string.web_player_buffering),
                testTag = PlayerTestTags.Buffering,
            )
        }

        state.isEnded -> {
            StreamCoreWebPanel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = WebPlayerTokens.StatusVerticalOffset)
                    .zIndex(WebPlayerZOrder.Status)
                    .widthIn(max = WebPlayerTokens.StatusPanelMaxWidth)
                    .testTag(WebPlayerTestTags.Ended),
            ) {
                Text(
                    text = stringResource(Res.string.web_player_ended),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        state.requiresUserActivation() -> {
            StreamCoreWebPanel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = WebPlayerTokens.StatusVerticalOffset)
                    .zIndex(WebPlayerZOrder.Status)
                    .widthIn(max = WebPlayerTokens.StatusPanelMaxWidth)
                    .testTag(WebPlayerTestTags.Activation),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
                    Text(
                        text = stringResource(Res.string.web_player_activation_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.web_player_activation_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.WebPlayerProgressStatus(
    text: String,
    testTag: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(WebPlayerZOrder.Status)
            .testTag(testTag),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(WebPlayerTokens.StatusProgressSize),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun BoxScope.WebPlayerSeekFeedback(seconds: Int) {
    val feedback = if (seconds < 0) {
        stringResource(
            Res.string.web_player_seek_feedback_back,
            seconds.absoluteValue,
        )
    } else {
        stringResource(Res.string.web_player_seek_feedback_forward, seconds)
    }
    Text(
        text = feedback,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .align(if (seconds < 0) Alignment.CenterStart else Alignment.CenterEnd)
            .zIndex(WebPlayerZOrder.SeekFeedback)
            .padding(horizontal = WebPlayerTokens.SeekFeedbackHorizontalPadding)
            .testTag(WebPlayerTestTags.SeekFeedback),
    )
}

private data class WebPlayerFilmstripItem(
    val key: String,
    val frame: com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel,
    val centered: Boolean,
)

private fun handleRootKeyEvent(
    event: KeyEvent,
    controlsVisible: Boolean,
    isFullscreen: Boolean,
    onExitFullscreen: () -> Unit,
    onAction: (PlayerAction) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) {
        return false
    }
    if (event.key == Key.Escape) {
        if (isFullscreen) {
            onExitFullscreen()
        } else {
            onAction(PlayerAction.BackSelected)
        }
        return true
    }
    if (controlsVisible || event.key !in WebPlayerRevealKeys) {
        return false
    }
    onAction(PlayerAction.UserInteraction)
    return true
}

private fun handleTimelineKeyEvent(
    event: KeyEvent,
    state: PlayerUiState,
    playFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    onAction: (PlayerAction) -> Unit,
): Boolean {
    if (!state.canSeek) {
        return false
    }
    if (
        event.type == KeyEventType.KeyUp &&
        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
    ) {
        return true
    }
    if (event.type != KeyEventType.KeyDown) {
        return false
    }
    val delta = when (event.key) {
        Key.DirectionUp -> {
            return playFocusRequester.requestFocus()
        }

        Key.DirectionDown -> {
            return settingsFocusRequester.requestFocus()
        }

        Key.DirectionLeft -> -WebPlayerTokens.SeekIntervalMillis
        Key.DirectionRight -> WebPlayerTokens.SeekIntervalMillis
        else -> return false
    }
    val target = (state.displayPositionMillis() + delta).coerceIn(0L, state.durationMillis)
    onAction(PlayerAction.ScrubStarted)
    onAction(PlayerAction.ScrubChanged(target))
    onAction(PlayerAction.ScrubFinished)
    return true
}

private fun Modifier.webPlayerDirectionalFocus(
    up: FocusRequester? = null,
    down: FocusRequester? = null,
    left: FocusRequester? = null,
    right: FocusRequester? = null,
): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) {
            return@onPreviewKeyEvent false
        }
        val requester = when (event.key) {
            Key.DirectionUp -> up
            Key.DirectionDown -> down
            Key.DirectionLeft -> left
            Key.DirectionRight -> right
            else -> null
        } ?: return@onPreviewKeyEvent false
        return@onPreviewKeyEvent requester.requestFocus()
    }
}

private fun PlayerUiState.displayPositionMillis(): Long {
    return if (isScrubbing) scrubPositionMillis else positionMillis
}

private fun PlayerUiState.requiresUserActivation(): Boolean {
    return phase == PlaybackPhase.Ready && !isPlaying && positionMillis == 0L
}

internal fun formatPlaybackTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

internal suspend fun FocusRequester.requestFocusWhenReady(): Boolean {
    repeat(WebPlayerTokens.FocusRequestAttempts) {
        withFrameNanos { }
        if (requestFocus()) {
            return true
        }
    }
    return false
}

private val WebPlayerRevealKeys = setOf(
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.DirectionUp,
    Key.DirectionDown,
    Key.Enter,
    Key.Spacebar,
)

@Composable
private fun WebPlayerPreview(scenario: WebPlayerShowcaseScenario) {
    StreamCoreTheme(darkTheme = true) {
        WebPlayerScreen(
            state = WebPlayerFixtures.state(scenario),
            videoSurface = WebPlayerFixtures.videoSurface,
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerPlayingPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.Playing)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerActivationPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.AutoplayActivation)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerBufferingPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.Buffering)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerEndedPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.Ended)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerSettingsPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.Settings)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerRecoverableErrorPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.RecoverableError)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerNoFilmstripPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.NoFilmstrip)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerLongTextPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.LongText)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerControlsHiddenPreview() {
    WebPlayerPreview(WebPlayerShowcaseScenario.ControlsHidden)
}
