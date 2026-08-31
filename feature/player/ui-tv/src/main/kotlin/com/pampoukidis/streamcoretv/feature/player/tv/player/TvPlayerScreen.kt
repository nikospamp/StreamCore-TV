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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.extensions.onPlayerSurface
import com.pampoukidis.streamcoretv.core.ui.extensions.playerSurface
import com.pampoukidis.streamcoretv.core.ui.extensions.playerThumbnailPlaceholder
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
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
    val forwardFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var previousSettingsPage by remember { mutableStateOf<PlayerSettingsPage?>(null) }
    var controlsWereVisible by remember { mutableStateOf(state.controlsVisible) }
    var playbackFocusInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(state.controlsVisible, state.phase) {
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

    }

    val error = state.error
    if (error != null) {
        TvPlayerErrorDialog(
            message = error.message,
            isRecoverable = error.isRecoverable,
            onRetry = { onAction(PlayerAction.Retry) },
            onBack = { onAction(PlayerAction.BackSelected) },
        )
    } else {
        state.settingsPage?.let { settingsPage ->
        TvPlayerSettingsDialog(
            state = state,
            page = settingsPage,
            onAction = onAction,
        )
        }
    }
}

@Composable
private fun BoxScope.TvPlayerControls(
    state: PlayerUiState,
    backFocusRequester: FocusRequester,
    rewindFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    forwardFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    onAction: (PlayerAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Spacing.Large,
            ),
    ) {
        StreamCoreTvButton(
            text = "Back",
            onClick = { onAction(PlayerAction.BackSelected) },
            enabled = true,
            variant = StreamCoreTvButtonVariant.Tertiary,
            leadingIcon = { StreamCoreBackIcon() },
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .focusProperties {
                    right = FocusRequester.Cancel
                    down = playFocusRequester
                }
                .testTag(PlayerTestTags.Back),
        )
        Text(
            text = state.title,
            color = MaterialTheme.colorScheme.onPlayerSurface,
            style = MaterialTheme.typography.headlineSmall,
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
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Spacing.Large,
            ),
    ) {
        if (state.isScrubbing) {
            TvPlayerFilmstrip(state)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StreamCoreTvButton(
                text = "Back 10 seconds",
                onClick = { onAction(PlayerAction.SeekBy(-SeekIntervalMillis, showFeedback = true)) },
                enabled = state.canSeek,
                variant = StreamCoreTvButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(rewindFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                        left = FocusRequester.Cancel
                        right = playFocusRequester
                        down = timelineFocusRequester
                    }
                    .testTag(PlayerTestTags.Rewind),
            )
            val playPauseLabel = when {
                    state.isEnded -> "Replay"
                    state.isPlaying -> "Pause"
                    else -> "Play"
                }
            StreamCoreTvButton(
                text = playPauseLabel,
                onClick = { onAction(PlayerAction.TogglePlayPause) },
                enabled = state.phase != PlaybackPhase.Preparing,
                loading = state.phase == PlaybackPhase.Preparing || state.isBuffering,
                variant = StreamCoreTvButtonVariant.Primary,
                leadingIcon = if (state.isPlaying || state.phase == PlaybackPhase.Preparing) {
                    null
                } else {
                    { StreamCorePlayIcon() }
                },
                modifier = Modifier
                    .focusRequester(playFocusRequester)
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
            StreamCoreTvButton(
                text = "Forward 10 seconds",
                onClick = { onAction(PlayerAction.SeekBy(SeekIntervalMillis, showFeedback = true)) },
                enabled = state.canSeek,
                variant = StreamCoreTvButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(forwardFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                        left = playFocusRequester
                        right = FocusRequester.Cancel
                        down = timelineFocusRequester
                    }
                    .testTag(PlayerTestTags.Forward),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${formatTime(state.positionMillis)} / ${formatTime(state.durationMillis)}",
                color = MaterialTheme.colorScheme.onPlayerSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Box(contentAlignment = Alignment.Center) {
            LinearProgressIndicator(
                progress = {
                    if (state.durationMillis > 0L) {
                        (state.bufferedPositionMillis.toFloat() / state.durationMillis).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            val duration = state.durationMillis.coerceAtLeast(1L)
            val position = if (state.isScrubbing) state.scrubPositionMillis else state.positionMillis
            Slider(
                value = position.toFloat().coerceIn(0f, duration.toFloat()),
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
                    .focusRequester(timelineFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }
                        when (event.key) {
                            Key.DirectionUp -> {
                                playFocusRequester.requestFocus()
                                true
                            }

                            Key.DirectionDown -> {
                                settingsFocusRequester.requestFocus()
                                true
                            }

                            else -> false
                        }
                    }
                    .focusProperties {
                        up = playFocusRequester
                        down = settingsFocusRequester
                    }
                    .testTag(PlayerTestTags.Timeline),
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StreamCoreTvButton(
                text = "Playback settings",
                onClick = { onAction(PlayerAction.OpenSettings()) },
                enabled = state.phase != PlaybackPhase.Preparing,
                variant = StreamCoreTvButtonVariant.Tertiary,
                modifier = Modifier
                    .focusRequester(settingsFocusRequester)
                    .focusProperties {
                        up = timelineFocusRequester
                        down = FocusRequester.Cancel
                    }
                    .testTag(PlayerTestTags.SettingsButton),
            )
        }
    }

    if (state.phase == PlaybackPhase.Preparing || state.isBuffering) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(PlayerTestTags.Buffering),
        )
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
private fun TvPlayerSettingsDialog(
    state: PlayerUiState,
    page: PlayerSettingsPage,
    onAction: (PlayerAction) -> Unit,
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val items = remember(state, page, onAction) {
        settingsItems(state, page, onAction)
    }

    LaunchedEffect(page, items.firstOrNull()?.key) {
        if (items.isNotEmpty()) {
            firstItemFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = { onAction(PlayerAction.CloseSettings) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .width(StreamCoreDimens.Tv.Player.SettingsPanelWidth)
                    .heightIn(max = StreamCoreDimens.Tv.Player.SettingsPanelMaxHeight)
                    .fillMaxHeight()
                    .testTag(PlayerTestTags.Settings),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                    modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge),
                ) {
                    Text(
                        text = settingsTitle(page),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = if (page == PlayerSettingsPage.Root) {
                            "Choose a playback preference"
                        } else {
                            "Press Back to return to playback settings"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.key },
                            contentType = { _, _ -> "tv-player-setting" },
                        ) { index, item ->
                            StreamCoreTvButton(
                                text = item.label,
                                onClick = item.onClick,
                                enabled = true,
                                selected = item.selected,
                                variant = StreamCoreTvButtonVariant.Secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(firstItemFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .semantics {
                                        if (page != PlayerSettingsPage.Root) {
                                            selected = item.selected
                                            role = Role.RadioButton
                                        }
                                    }
                                    .testTag(PlayerTestTags.SettingsOptionPrefix + item.key),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TvPlayerSettingItem(
    val key: String,
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

private fun settingsItems(
    state: PlayerUiState,
    page: PlayerSettingsPage,
    onAction: (PlayerAction) -> Unit,
): List<TvPlayerSettingItem> {
    return when (page) {
        PlayerSettingsPage.Root -> buildList {
            if (state.videoTracks.isNotEmpty()) {
                add(
                    TvPlayerSettingItem(
                        key = "quality",
                        label = "Quality · ${selectedLabel(state.videoTracks, state.selectedVideoTrackId) ?: "Auto"}",
                        onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Quality)) },
                    ),
                )
            }
            if (state.audioTracks.size > 1) {
                add(
                    TvPlayerSettingItem(
                        key = "audio",
                        label = "Audio · ${selectedLabel(state.audioTracks, state.selectedAudioTrackId) ?: "Default"}",
                        onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Audio)) },
                    ),
                )
            }
            if (state.textTracks.isNotEmpty()) {
                add(
                    TvPlayerSettingItem(
                        key = "subtitles",
                        label = "Subtitles · ${selectedLabel(state.textTracks, state.selectedTextTrackId) ?: "Off"}",
                        onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Subtitles)) },
                    ),
                )
            }
            add(
                TvPlayerSettingItem(
                    key = "speed",
                    label = "Speed · ${state.speed}×",
                    onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Speed)) },
                ),
            )
            add(
                TvPlayerSettingItem(
                    key = "resize",
                    label = "Resize mode · ${state.resizeMode.name}",
                    onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.ResizeMode)) },
                ),
            )
        }

        PlayerSettingsPage.Quality -> trackItems(
            tracks = state.videoTracks,
            selectedId = state.selectedVideoTrackId,
            automaticLabel = "Auto",
            onSelected = { onAction(PlayerAction.SelectVideoTrack(it)) },
        )

        PlayerSettingsPage.Audio -> trackItems(
            tracks = state.audioTracks,
            selectedId = state.selectedAudioTrackId,
            onSelected = { onAction(PlayerAction.SelectAudioTrack(it)) },
        )

        PlayerSettingsPage.Subtitles -> trackItems(
            tracks = state.textTracks,
            selectedId = state.selectedTextTrackId,
            automaticLabel = "Off",
            onSelected = { onAction(PlayerAction.SelectTextTrack(it)) },
        )

        PlayerSettingsPage.Speed -> PlaybackSpeedOptions.map { option ->
            TvPlayerSettingItem(
                key = option.second.toString(),
                label = option.first,
                selected = option.second == state.speed,
                onClick = { onAction(PlayerAction.SelectSpeed(option.second)) },
            )
        }

        PlayerSettingsPage.ResizeMode -> PlaybackResizeMode.entries.map { mode ->
            TvPlayerSettingItem(
                key = mode.name,
                label = mode.name,
                selected = mode == state.resizeMode,
                onClick = { onAction(PlayerAction.SelectResizeMode(mode)) },
            )
        }
    }
}

private fun trackItems(
    tracks: List<PlaybackTrackModel>,
    selectedId: String?,
    automaticLabel: String? = null,
    onSelected: (String?) -> Unit,
): List<TvPlayerSettingItem> {
    return buildList {
        automaticLabel?.let { label ->
            add(
                TvPlayerSettingItem(
                    key = "automatic",
                    label = label,
                    selected = selectedId == null,
                    onClick = { onSelected(null) },
                ),
            )
        }
        tracks.forEach { track ->
            add(
                TvPlayerSettingItem(
                    key = track.id,
                    label = track.label,
                    selected = track.id == selectedId,
                    onClick = { onSelected(track.id) },
                ),
            )
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

private fun settingsTitle(page: PlayerSettingsPage): String {
    return when (page) {
        PlayerSettingsPage.Root -> "Playback settings"
        PlayerSettingsPage.Quality -> "Quality"
        PlayerSettingsPage.Audio -> "Audio"
        PlayerSettingsPage.Subtitles -> "Subtitles"
        PlayerSettingsPage.Speed -> "Speed"
        PlayerSettingsPage.ResizeMode -> "Resize mode"
    }
}

private fun selectedLabel(tracks: List<PlaybackTrackModel>, id: String?): String? {
    return tracks.firstOrNull { it.id == id }?.label
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
private val PlaybackSpeedOptions = listOf(
    "0.5×" to 0.5f,
    "0.75×" to 0.75f,
    "1.0×" to 1f,
    "1.25×" to 1.25f,
    "1.5×" to 1.5f,
    "2.0×" to 2f,
)

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
