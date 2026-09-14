package com.pampoukidis.streamcoretv.feature.player.tv.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvActionSurface
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvArtworkIconButton
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

/** In-composition panel keeps the playing video visible through the same scrim as touch. */
@Composable
internal fun TvPlayerSettingsOverlay(
    state: PlayerUiState,
    page: PlayerSettingsPage,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onAction(PlayerAction.BackSelected) }
    val items = remember(
        page, state.videoTracks, state.audioTracks, state.textTracks,
        state.selectedVideoTrackId, state.selectedAudioTrackId, state.selectedTextTrackId,
        state.speed, state.resizeMode,
    ) {
        settingsItems(state, page)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SettingsScrimAlpha)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SettingsPanelAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxHeight()
                .width(StreamCoreDimens.Tv.Player.SettingsPanelWidth)
                .testTag(PlayerTestTags.Settings)
                .focusProperties {
                    onExit = { cancelFocusChange() }
                }
                .focusGroup()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Back || event.key == Key.Escape) {
                        if (event.type == KeyEventType.KeyDown) {
                            onAction(PlayerAction.BackSelected)
                        }
                        true
                    } else {
                        false
                    }
                },
        ) {
            key(page) {
                val firstItemFocusRequester = remember { FocusRequester() }
                val backFocusRequester = remember { FocusRequester() }
                LaunchedEffect(items.firstOrNull()?.key) {
                    if (items.isNotEmpty()) {
                        firstItemFocusRequester.requestFocus()
                    } else {
                        backFocusRequester.requestFocus()
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = StreamCoreDimens.Tv.Player.SettingsSafeInset),
                ) {
                    PlayerSettingsHeaderContent(
                        page = page,
                        titleStyle = MaterialTheme.typography.headlineSmall,
                        supportingStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .height(StreamCoreDimens.Tv.Player.SettingsHeaderHeight)
                            .padding(horizontal = StreamCoreDimens.Tv.Player.SettingsHorizontalPadding),
                    ) {
                        if (page == PlayerSettingsPage.Root) {
                            PlayerSettingsIconContainer(
                                icon = PlayerSettingsIconType.Settings,
                                tint = MaterialTheme.colorScheme.onSurface,
                                containerSize = StreamCoreDimens.Tv.Player.SettingsIconContainerSize,
                                iconSize = StreamCoreDimens.Tv.Player.SettingsIconSize,
                            )
                        } else {
                            StreamCoreTvArtworkIconButton(
                                contentDescription = "Back to settings",
                                onClick = { onAction(PlayerAction.BackSelected) },
                                modifier = Modifier.focusRequester(backFocusRequester),
                            ) {
                                StreamCoreBackIcon()
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SettingsDividerAlpha),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = StreamCoreDimens.Tv.Player.SettingsHorizontalPadding,
                            end = StreamCoreDimens.Tv.Player.SettingsHorizontalPadding,
                            top = StreamCoreDimens.Spacing.Small,
                            bottom = StreamCoreDimens.Tv.Player.SettingsSafeInset,
                        ),
                        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.key },
                            contentType = { _, item -> if (item.icon != null) "settings-navigation" else "settings-selection" },
                        ) { index, item ->
                            TvSettingsRow(
                                item = item,
                                onClick = { onAction(item.action) },
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstItemFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSettingsRow(
    item: TvPlayerSettingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        StreamCoreTvActionSurface(
            onClick = onClick,
            enabled = true,
            role = if (item.icon == null) Role.RadioButton else Role.Button,
            modifier = modifier
                .fillMaxWidth()
                .height(
                    if (item.icon == null) StreamCoreDimens.Tv.Player.SettingsSelectionRowHeight
                    else StreamCoreDimens.Tv.Player.SettingsRowHeight,
                )
                .semantics {
                    if (item.icon == null) selected = item.selected
                }
                .testTag(PlayerTestTags.SettingsOptionPrefix + item.key),
        ) {
            if (item.icon != null) {
                PlayerSettingsNavigationContent(
                    title = item.label,
                    value = item.value,
                    icon = item.icon,
                    iconContainerSize = StreamCoreDimens.Tv.Player.SettingsIconContainerSize,
                    iconSize = StreamCoreDimens.Tv.Player.SettingsIconSize,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    valueStyle = MaterialTheme.typography.bodyLarge,
                    valueModifier = Modifier.widthIn(max = StreamCoreDimens.Tv.Player.SettingsValueMaxWidth),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = StreamCoreDimens.Spacing.Small),
                )
            } else {
                Surface(
                    color = if (item.selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = SelectedRowAlpha)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    },
                    contentColor = if (item.selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(StreamCoreDimens.Tv.Focus.BorderWidth),
                ) {
                    PlayerSettingsSelectionContent(
                        label = item.label,
                        selected = item.selected,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = StreamCoreDimens.Spacing.Large),
                    )
                }
            }
        }
        if (item.icon != null) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SettingsDividerAlpha),
                modifier = Modifier.padding(
                    start = StreamCoreDimens.Spacing.Small +
                        StreamCoreDimens.Tv.Player.SettingsIconContainerSize +
                        StreamCoreDimens.Spacing.Large,
                ),
            )
        }
    }
}

private data class TvPlayerSettingItem(
    val key: String,
    val label: String,
    val action: PlayerAction,
    val value: String? = null,
    val icon: PlayerSettingsIconType? = null,
    val selected: Boolean = false,
)

private fun settingsItems(
    state: PlayerUiState,
    page: PlayerSettingsPage,
): List<TvPlayerSettingItem> {
    return when (page) {
        PlayerSettingsPage.Root -> buildList {
            if (state.videoTracks.isNotEmpty()) {
                add(TvPlayerSettingItem(
                    key = "quality", label = "Quality",
                    value = selectedLabel(state.videoTracks, state.selectedVideoTrackId) ?: "Auto",
                    icon = PlayerSettingsIconType.Quality,
                    action = PlayerAction.OpenSettings(PlayerSettingsPage.Quality),
                ))
            }
            if (state.audioTracks.size > 1) {
                add(TvPlayerSettingItem(
                    key = "audio", label = "Audio",
                    value = selectedLabel(state.audioTracks, state.selectedAudioTrackId) ?: "Default",
                    icon = PlayerSettingsIconType.Audio,
                    action = PlayerAction.OpenSettings(PlayerSettingsPage.Audio),
                ))
            }
            if (state.textTracks.isNotEmpty()) {
                add(TvPlayerSettingItem(
                    key = "subtitles", label = "Subtitles",
                    value = selectedLabel(state.textTracks, state.selectedTextTrackId) ?: "Off",
                    icon = PlayerSettingsIconType.Subtitles,
                    action = PlayerAction.OpenSettings(PlayerSettingsPage.Subtitles),
                ))
            }
            add(TvPlayerSettingItem(
                key = "speed", label = "Speed", value = "${state.speed}×",
                icon = PlayerSettingsIconType.Speed,
                action = PlayerAction.OpenSettings(PlayerSettingsPage.Speed),
            ))
            add(TvPlayerSettingItem(
                key = "resize", label = "Resize mode", value = state.resizeMode.name,
                icon = PlayerSettingsIconType.ResizeMode,
                action = PlayerAction.OpenSettings(PlayerSettingsPage.ResizeMode),
            ))
        }
        PlayerSettingsPage.Quality -> trackItems(
            state.videoTracks, state.selectedVideoTrackId, "Auto",
            onSelected = { PlayerAction.SelectVideoTrack(it) },
        )
        PlayerSettingsPage.Audio -> trackItems(
            state.audioTracks, state.selectedAudioTrackId,
            onSelected = { PlayerAction.SelectAudioTrack(it) },
        )
        PlayerSettingsPage.Subtitles -> trackItems(
            state.textTracks, state.selectedTextTrackId, "Off",
            onSelected = { PlayerAction.SelectTextTrack(it) },
        )
        PlayerSettingsPage.Speed -> PlayerSettingsSpeedOptions.map { (label, speed) ->
            TvPlayerSettingItem(
                key = speed.toString(), label = label, selected = speed == state.speed,
                action = PlayerAction.SelectSpeed(speed),
            )
        }
        PlayerSettingsPage.ResizeMode -> PlayerSettingsResizeModeOptions.map { (label, mode) ->
            TvPlayerSettingItem(
                key = mode.name, label = label, selected = mode == state.resizeMode,
                action = PlayerAction.SelectResizeMode(mode),
            )
        }
    }
}

private fun trackItems(
    tracks: List<PlaybackTrackModel>,
    selectedId: String?,
    automaticLabel: String? = null,
    onSelected: (String?) -> PlayerAction,
): List<TvPlayerSettingItem> {
    return buildList {
        automaticLabel?.let { label ->
            add(TvPlayerSettingItem(
                key = "automatic", label = label, selected = selectedId == null,
                action = onSelected(null),
            ))
        }
        tracks.forEach { track ->
            add(TvPlayerSettingItem(
                key = track.id, label = track.label, selected = track.id == selectedId,
                action = onSelected(track.id),
            ))
        }
    }
}

private fun selectedLabel(tracks: List<PlaybackTrackModel>, id: String?): String? {
    return tracks.firstOrNull { it.id == id }?.label
}

private const val SettingsScrimAlpha = 0.46f
private const val SettingsPanelAlpha = 0.995f
private const val SettingsDividerAlpha = 0.5f
private const val SelectedRowAlpha = 0.72f
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun TvPlayerSettingsOverlayPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvPlayerSettingsOverlay(
            state = PlayerUiState(
                videoTracks = listOf(PlaybackTrackModel("1080p", PlaybackTrackType.Video, "1080p")),
                audioTracks = listOf(
                    PlaybackTrackModel("en", PlaybackTrackType.Audio, "English"),
                    PlaybackTrackModel("fr", PlaybackTrackType.Audio, "French"),
                ),
                textTracks = listOf(PlaybackTrackModel("text-en", PlaybackTrackType.Text, "English")),
            ),
            page = PlayerSettingsPage.Root,
            onAction = {},
        )
    }
}
