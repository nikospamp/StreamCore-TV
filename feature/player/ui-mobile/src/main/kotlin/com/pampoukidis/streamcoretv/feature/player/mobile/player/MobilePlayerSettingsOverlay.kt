package com.pampoukidis.streamcoretv.feature.player.mobile.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsHeaderContent
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsIconContainer
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsIconType
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsNavigationContent
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsSpeedOptions
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsResizeModeOptions
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsSelectionContent
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.PlayerSettingsOverlay(
    state: PlayerUiState,
    page: PlayerSettingsPage?,
    onAction: (PlayerAction) -> Unit,
) {
    var displayedPage by remember { mutableStateOf(page ?: PlayerSettingsPage.Root) }
    LaunchedEffect(page) {
        if (page != null) {
            displayedPage = page
        }
    }

    AnimatedVisibility(
        visible = page != null,
        enter = fadeIn(animationSpec = tween(SettingsEnterDurationMillis)),
        exit = fadeOut(animationSpec = tween(SettingsExitDurationMillis)),
        modifier = Modifier.align(Alignment.Center),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SettingsScrimAlpha))
                .clickable(
                    role = Role.Button,
                    onClickLabel = page.dismissLabel(),
                    onClick = { onAction(PlayerAction.BackSelected) },
                )
                .semantics {
                    contentDescription = page.dismissLabel()
                },
        )
    }

    AnimatedVisibility(
        visible = page != null,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = SettingsEnterDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetX = { it },
        ) + fadeIn(animationSpec = tween(SettingsEnterDurationMillis)),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = SettingsExitDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            targetOffsetX = { it },
        ) + fadeOut(animationSpec = tween(SettingsExitDurationMillis)),
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        PlayerSettingsPanel(
            state = state,
            page = displayedPage,
            onAction = onAction,
        )
    }
}

@Composable
private fun PlayerSettingsPanel(
    state: PlayerUiState,
    page: PlayerSettingsPage,
    onAction: (PlayerAction) -> Unit,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val panelWidth = (screenWidthDp * SettingsPanelWidthFraction)
        .roundToInt()
        .coerceIn(SettingsPanelMinWidthDp, SettingsPanelMaxWidthDp)
        .coerceAtMost(screenWidthDp)
        .dp

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SettingsPanelAlpha),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .testTag(PlayerTestTags.Settings)
            .fillMaxHeight()
            .width(panelWidth)
            .navigationBarsPadding(),
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val isReturningToRoot = targetState == PlayerSettingsPage.Root
                val enter = slideInHorizontally(
                    animationSpec = tween(SettingsPageDurationMillis),
                    initialOffsetX = { width -> if (isReturningToRoot) -width / 5 else width / 5 },
                ) + fadeIn(animationSpec = tween(SettingsPageDurationMillis))
                val exit = slideOutHorizontally(
                    animationSpec = tween(SettingsPageExitDurationMillis),
                    targetOffsetX = { width -> if (isReturningToRoot) width / 5 else -width / 5 },
                ) + fadeOut(animationSpec = tween(SettingsPageExitDurationMillis))
                enter.togetherWith(exit).using(SizeTransform(clip = false))
            },
            label = "player-settings-page",
            modifier = Modifier.fillMaxSize(),
        ) { targetPage ->
            PlayerSettingsPageContent(
                state = state,
                page = targetPage,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PlayerSettingsPageContent(
    state: PlayerUiState,
    page: PlayerSettingsPage,
    onAction: (PlayerAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlayerSettingsHeader(
            page = page,
            onBack = { onAction(PlayerAction.CloseSettings) },
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SettingsDividerAlpha),
        )
        when (page) {
            PlayerSettingsPage.Root -> SettingsRoot(state, onAction)
            PlayerSettingsPage.Quality -> TrackSettings(
                tracks = state.videoTracks,
                selectedId = state.selectedVideoTrackId,
                automaticOption = "Auto",
                onSelected = { onAction(PlayerAction.SelectVideoTrack(it)) },
            )

            PlayerSettingsPage.Audio -> TrackSettings(
                tracks = state.audioTracks,
                selectedId = state.selectedAudioTrackId,
                onSelected = { onAction(PlayerAction.SelectAudioTrack(it)) },
            )

            PlayerSettingsPage.Subtitles -> TrackSettings(
                tracks = state.textTracks,
                selectedId = state.selectedTextTrackId,
                automaticOption = "Off",
                onSelected = { onAction(PlayerAction.SelectTextTrack(it)) },
            )

            PlayerSettingsPage.Speed -> SpeedSettings(state, onAction)
            PlayerSettingsPage.ResizeMode -> ResizeSettings(state, onAction)
        }
    }
}

@Composable
private fun PlayerSettingsHeader(
    page: PlayerSettingsPage,
    onBack: () -> Unit,
) {
    PlayerSettingsHeaderContent(
        page = page,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Spacing.ExtraLarge)
            .height(SettingsHeaderHeight),
    ) {
        if (page == PlayerSettingsPage.Root) {
            PlayerSettingsIconContainer(
                icon = PlayerSettingsIconType.Settings,
                tint = MaterialTheme.colorScheme.onSurface,
                containerSize = SettingsIconContainerSize,
                iconSize = SettingsIconSize,
            )
        } else {
            StreamCoreArtworkIconButton(
                contentDescription = "Back to settings",
                onClick = onBack,
            ) {
                StreamCoreBackIcon()
            }
        }
    }
}

@Composable
private fun SettingsRoot(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = StreamCoreDimens.Spacing.Small,
            bottom = StreamCoreDimens.Spacing.ExtraLarge,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.videoTracks.isNotEmpty()) {
            item(key = "quality", contentType = "settings-navigation") {
                SettingsNavigationRow(
                    title = "Quality",
                    value = selectedVideoLabel(state),
                    icon = PlayerSettingsIconType.Quality,
                    onClick = {
                        onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Quality))
                    },
                )
            }
        }
        if (state.audioTracks.size > 1) {
            item(key = "audio", contentType = "settings-navigation") {
                SettingsNavigationRow(
                    title = "Audio",
                    value = selectedLabel(state.audioTracks, state.selectedAudioTrackId),
                    icon = PlayerSettingsIconType.Audio,
                    onClick = {
                        onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Audio))
                    },
                )
            }
        }
        if (state.textTracks.isNotEmpty()) {
            item(key = "subtitles", contentType = "settings-navigation") {
                SettingsNavigationRow(
                    title = "Subtitles",
                    value = selectedLabel(state.textTracks, state.selectedTextTrackId) ?: "Off",
                    icon = PlayerSettingsIconType.Subtitles,
                    onClick = {
                        onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Subtitles))
                    },
                )
            }
        }
        item(key = "speed", contentType = "settings-navigation") {
            SettingsNavigationRow(
                title = "Speed",
                value = "${state.speed}×",
                icon = PlayerSettingsIconType.Speed,
                onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Speed)) },
            )
        }
        item(key = "resize-mode", contentType = "settings-navigation") {
            SettingsNavigationRow(
                title = "Resize mode",
                value = state.resizeMode.name,
                icon = PlayerSettingsIconType.ResizeMode,
                onClick = {
                    onAction(PlayerAction.OpenSettings(PlayerSettingsPage.ResizeMode))
                },
            )
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    value: String?,
    icon: PlayerSettingsIconType,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.transparentContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsRowHeight),
    ) {
        PlayerSettingsNavigationContent(
            title = title,
            value = value,
            icon = icon,
            iconContainerSize = SettingsIconContainerSize,
            iconSize = SettingsIconSize,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Spacing.ExtraLarge),
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SettingsDividerAlpha),
        modifier = Modifier.padding(start = SettingsDividerStartPadding),
    )
}

@Composable
private fun TrackSettings(
    tracks: List<PlaybackTrackModel>,
    selectedId: String?,
    automaticOption: String? = null,
    onSelected: (String?) -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = StreamCoreDimens.Spacing.Medium,
            vertical = StreamCoreDimens.Spacing.Medium,
        ),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
        modifier = Modifier.fillMaxSize(),
    ) {
        automaticOption?.let { label ->
            item(key = "automatic-option", contentType = "settings-selection") {
                SelectionRow(
                    label = label,
                    selected = selectedId == null,
                    onClick = { onSelected(null) },
                )
            }
        }
        items(
            items = tracks,
            key = { it.id },
            contentType = { "settings-selection" },
        ) { track ->
            SelectionRow(
                label = track.label,
                selected = track.id == selectedId,
                onClick = { onSelected(track.id) },
            )
        }
    }
}

@Composable
private fun SpeedSettings(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    SelectionSettings(
        options = PlayerSettingsSpeedOptions,
        selected = state.speed,
        onSelected = { onAction(PlayerAction.SelectSpeed(it)) },
    )
}

@Composable
private fun ResizeSettings(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    SelectionSettings(
        options = PlayerSettingsResizeModeOptions,
        selected = state.resizeMode,
        onSelected = { onAction(PlayerAction.SelectResizeMode(it)) },
    )
}

@Composable
private fun <T> SelectionSettings(
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = StreamCoreDimens.Spacing.Medium,
            vertical = StreamCoreDimens.Spacing.Medium,
        ),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = options,
            key = { it.second.toString() },
            contentType = { "settings-selection" },
        ) { option ->
            SelectionRow(
                label = option.first,
                selected = option.second == selected,
                onClick = { onSelected(option.second) },
            )
        }
    }
}

@Composable
private fun SelectionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = SelectedRowAlpha)
    } else {
        MaterialTheme.colorScheme.transparentContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        PlayerSettingsSelectionContent(
            label = label,
            selected = selected,
            modifier = Modifier
                .fillMaxWidth()
                .height(SettingsSelectionRowHeight)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(horizontal = StreamCoreDimens.Spacing.Large),
        )
    }
}
private fun PlayerSettingsPage?.dismissLabel(): String {
    return if (this == PlayerSettingsPage.Root) {
        "Dismiss playback settings"
    } else {
        "Back to settings"
    }
}

private fun selectedVideoLabel(state: PlayerUiState): String {
    return selectedLabel(state.videoTracks, state.selectedVideoTrackId) ?: "Auto"
}

private fun selectedLabel(
    tracks: List<PlaybackTrackModel>,
    id: String?,
): String? {
    return tracks.firstOrNull { it.id == id }?.label
}

private const val SettingsEnterDurationMillis = 240
private const val SettingsExitDurationMillis = 180
private const val SettingsPageDurationMillis = 200
private const val SettingsPageExitDurationMillis = 150
private const val SettingsPanelWidthFraction = 0.4f
private const val SettingsPanelMinWidthDp = 320
private const val SettingsPanelMaxWidthDp = 420
private const val SettingsPanelAlpha = 0.995f
private const val SettingsScrimAlpha = 0.46f
private const val SettingsDividerAlpha = 0.5f
private const val SelectedRowAlpha = 0.72f
private val SettingsHeaderHeight = StreamCoreDimens.Mobile.Player.SettingsHeaderHeight
private val SettingsRowHeight = StreamCoreDimens.Mobile.Player.SettingsRowHeight
private val SettingsSelectionRowHeight = StreamCoreDimens.Mobile.Player.SettingsSelectionRowHeight
private val SettingsIconContainerSize = StreamCoreDimens.Mobile.Player.SettingsIconContainerSize
private val SettingsIconSize = StreamCoreDimens.Mobile.Player.SettingsIconSize
private val SettingsDividerStartPadding = StreamCoreDimens.Mobile.Player.SettingsDividerStartPadding
@Preview(widthDp = 800, heightDp = 560, showBackground = true)
@Composable
private fun PlayerSettingsOverlayPreview() {
    StreamCoreTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            PlayerSettingsOverlay(
                state = PlayerUiState(
                    videoTracks = listOf(
                        PlaybackTrackModel("video-1080", PlaybackTrackType.Video, "1080p"),
                    ),
                    audioTracks = listOf(
                        PlaybackTrackModel("audio-en", PlaybackTrackType.Audio, "English"),
                        PlaybackTrackModel("audio-fr", PlaybackTrackType.Audio, "French"),
                    ),
                    textTracks = listOf(
                        PlaybackTrackModel("text-en", PlaybackTrackType.Text, "English"),
                    ),
                    settingsPage = PlayerSettingsPage.Root,
                ),
                page = PlayerSettingsPage.Root,
                onAction = {},
            )
        }
    }
}
