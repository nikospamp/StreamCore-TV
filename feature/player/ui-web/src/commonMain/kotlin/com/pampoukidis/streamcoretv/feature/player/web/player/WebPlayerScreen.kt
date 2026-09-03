package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerFixtures
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerShowcaseScenario
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
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
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

@Composable
fun WebPlayerScreen(
    state: PlayerUiState,
    videoSurface: PlaybackVideoSurface?,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnAction by rememberUpdatedState(onAction)
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

    WebPlayerDocumentEscapeEffect {
        if (isFullscreen) {
            fullscreenController.exit()
        } else {
            currentOnAction(PlayerAction.BackSelected)
        }
    }

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
                    controlsVisible = state.controlsVisible,
                    isFullscreen = isFullscreen,
                    onExitFullscreen = fullscreenController::exit,
                    onAction = currentOnAction,
                )
            }
            .focusable()
            .webPlayerPointerInteraction {
                currentOnAction(PlayerAction.UserInteraction)
            },
    ) {
        WebPlayerVideoSurface(
            state = state,
            videoSurface = videoSurface,
            onClick = {
                currentOnAction(
                    if (state.controlsVisible) {
                        PlayerAction.ToggleControls
                    } else {
                        PlayerAction.UserInteraction
                    },
                )
            },
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
        WebPlayerErrorDialog(
            message = error.message,
            recoverable = error.isRecoverable,
            onAction = currentOnAction,
        )
    } else {
        state.settingsPage?.let { page ->
            WebPlayerSettingsDialog(
                state = state,
                page = page,
                onAction = currentOnAction,
            )
        }
    }
}

@Composable
private fun WebPlayerVideoSurface(
    state: PlayerUiState,
    videoSurface: PlaybackVideoSurface?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
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
    val scrimColor = MaterialTheme.colorScheme.scrim
    val transparentScrim = scrimColor.copy(alpha = WebPlayerTokens.SurfaceScrimTransparentAlpha)
    val opaqueScrim = scrimColor.copy(alpha = WebPlayerTokens.SurfaceScrimAlpha)
    val topBrush = remember(scrimColor) {
        Brush.verticalGradient(colors = listOf(opaqueScrim, transparentScrim))
    }
    val bottomBrush = remember(scrimColor) {
        Brush.verticalGradient(colors = listOf(transparentScrim, opaqueScrim))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .heightIn(min = WebPlayerTokens.TopBarMinHeight)
            .background(topBrush)
            .padding(
                horizontal = WebPlayerTokens.ScreenHorizontalPadding,
                vertical = WebPlayerTokens.ScreenVerticalPadding,
            )
            .testTag(WebPlayerTestTags.Controls),
    ) {
        WebPlayerControlButton(
            text = stringResource(Res.string.web_player_back),
            onClick = onBack,
            variant = StreamCoreWebButtonVariant.Tertiary,
            focusRequester = backFocusRequester,
            modifier = Modifier
                .webPlayerDirectionalFocus(down = playFocusRequester)
                .focusProperties {
                    right = FocusRequester.Cancel
                    down = playFocusRequester
                }
                .testTag(PlayerTestTags.Back),
        )
        Text(
            text = state.title.ifBlank {
                stringResource(Res.string.web_player_title_fallback)
            },
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(bottomBrush)
            .padding(
                start = WebPlayerTokens.ScreenHorizontalPadding,
                end = WebPlayerTokens.ScreenHorizontalPadding,
                top = WebPlayerTokens.ControlStripPadding,
                bottom = WebPlayerTokens.ScreenVerticalPadding,
            ),
    ) {
        if (state.isScrubbing) {
            WebPlayerFilmstrip(state = state)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            WebPlayerControlButton(
                text = stringResource(Res.string.web_player_rewind),
                onClick = {
                    onAction(
                        PlayerAction.SeekBy(
                            deltaMillis = -WebPlayerTokens.SeekIntervalMillis,
                            showFeedback = true,
                        ),
                    )
                },
                enabled = state.canSeek,
                variant = StreamCoreWebButtonVariant.Secondary,
                focusRequester = rewindFocusRequester,
                modifier = Modifier
                    .webPlayerDirectionalFocus(
                        up = backFocusRequester,
                        right = playFocusRequester,
                        down = timelineFocusRequester,
                    )
                    .focusProperties {
                        up = backFocusRequester
                        left = FocusRequester.Cancel
                        right = playFocusRequester
                        down = timelineFocusRequester
                    }
                    .testTag(PlayerTestTags.Rewind),
            )
            val playPauseLabel = when {
                state.isEnded -> stringResource(Res.string.web_player_replay)
                state.isPlaying -> stringResource(Res.string.web_player_pause)
                else -> stringResource(Res.string.web_player_play)
            }
            WebPlayerControlButton(
                text = playPauseLabel,
                onClick = { onAction(PlayerAction.TogglePlayPause) },
                enabled = state.phase != PlaybackPhase.Preparing,
                loading = state.phase == PlaybackPhase.Preparing || state.isBuffering,
                variant = StreamCoreWebButtonVariant.Primary,
                focusRequester = playFocusRequester,
                modifier = Modifier
                    .sizeIn(minWidth = WebPlayerTokens.PrimaryControlMinWidth)
                    .webPlayerDirectionalFocus(
                        up = backFocusRequester,
                        left = rewindFocusRequester,
                        right = forwardFocusRequester,
                        down = timelineFocusRequester,
                    )
                    .focusProperties {
                        up = backFocusRequester
                        left = rewindFocusRequester
                        right = forwardFocusRequester
                        down = timelineFocusRequester
                    }
                    .semantics(mergeDescendants = true) {
                        contentDescription = playPauseLabel
                        stateDescription = state.phase.name
                    }
                    .testTag(PlayerTestTags.PlayPause),
            )
            WebPlayerControlButton(
                text = stringResource(Res.string.web_player_forward),
                onClick = {
                    onAction(
                        PlayerAction.SeekBy(
                            deltaMillis = WebPlayerTokens.SeekIntervalMillis,
                            showFeedback = true,
                        ),
                    )
                },
                enabled = state.canSeek,
                variant = StreamCoreWebButtonVariant.Secondary,
                focusRequester = forwardFocusRequester,
                modifier = Modifier
                    .webPlayerDirectionalFocus(
                        up = backFocusRequester,
                        left = playFocusRequester,
                        right = fullscreenFocusRequester,
                        down = timelineFocusRequester,
                    )
                    .focusProperties {
                        up = backFocusRequester
                        left = playFocusRequester
                        right = fullscreenFocusRequester
                        down = timelineFocusRequester
                    }
                    .testTag(PlayerTestTags.Forward),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${formatPlaybackTime(state.displayPositionMillis())} / " +
                    formatPlaybackTime(state.durationMillis),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }

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
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            val fullscreenLabel = if (isFullscreen) {
                stringResource(Res.string.web_player_fullscreen_exit)
            } else {
                stringResource(Res.string.web_player_fullscreen_enter)
            }
            WebPlayerControlButton(
                text = fullscreenLabel,
                onClick = onFullscreen,
                variant = StreamCoreWebButtonVariant.Tertiary,
                focusRequester = fullscreenFocusRequester,
                modifier = Modifier
                    .webPlayerDirectionalFocus(
                        up = timelineFocusRequester,
                        left = forwardFocusRequester,
                        right = settingsFocusRequester,
                    )
                    .focusProperties {
                        up = timelineFocusRequester
                        left = forwardFocusRequester
                        right = settingsFocusRequester
                        down = FocusRequester.Cancel
                    }
                    .semantics(mergeDescendants = true) {
                        contentDescription = fullscreenLabel
                    }
                    .testTag(WebPlayerTestTags.Fullscreen),
            )
            WebPlayerControlButton(
                text = stringResource(Res.string.web_player_settings),
                onClick = { onAction(PlayerAction.OpenSettings()) },
                enabled = state.phase != PlaybackPhase.Preparing,
                variant = StreamCoreWebButtonVariant.Tertiary,
                focusRequester = settingsFocusRequester,
                modifier = Modifier
                    .webPlayerDirectionalFocus(
                        up = timelineFocusRequester,
                        left = fullscreenFocusRequester,
                    )
                    .focusProperties {
                        up = timelineFocusRequester
                        left = fullscreenFocusRequester
                        right = FocusRequester.Cancel
                        down = FocusRequester.Cancel
                    }
                    .testTag(PlayerTestTags.SettingsButton),
            )
        }
    }
}

@Composable
private fun WebPlayerControlButton(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: StreamCoreWebButtonVariant,
) {
    StreamCoreWebButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        variant = variant,
        modifier = modifier
            .sizeIn(minWidth = WebPlayerTokens.SecondaryControlMinWidth)
            .focusRequester(focusRequester),
    )
}

@Composable
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
        LinearProgressIndicator(
            progress = {
                if (state.durationMillis > 0L) {
                    (state.bufferedPositionMillis.toFloat() / state.durationMillis.toFloat())
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }
            },
            color = MaterialTheme.colorScheme.outline,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        )
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
                modifier = Modifier.testTag(WebPlayerTestTags.NoFilmstrip),
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
                                modifier = Modifier.fillMaxSize(),
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
        state.phase == PlaybackPhase.Idle || state.phase == PlaybackPhase.Preparing -> {
            WebPlayerProgressStatus(
                text = stringResource(Res.string.web_player_preparing),
                testTag = WebPlayerTestTags.Preparing,
            )
        }

        state.isBuffering -> {
            WebPlayerProgressStatus(
                text = stringResource(Res.string.web_player_buffering),
                testTag = PlayerTestTags.Buffering,
            )
        }

        state.isEnded -> {
            StreamCoreWebPanel(
                modifier = Modifier
                    .align(Alignment.Center)
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
            .testTag(testTag),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(WebPlayerTokens.StatusProgressSize),
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
