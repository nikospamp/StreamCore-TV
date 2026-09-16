package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebActionSurface
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsHeaderContent
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsIconContainer
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsIconType
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsNavigationContent
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsSelectionContent
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsSpeedOptions
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerFixtures
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerShowcaseScenario
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.player.ui_web.generated.resources.Res
import streamcoretv.feature.player.ui_web.generated.resources.web_player_audio
import streamcoretv.feature.player.ui_web.generated.resources.web_player_auto
import streamcoretv.feature.player.ui_web.generated.resources.web_player_back
import streamcoretv.feature.player.ui_web.generated.resources.web_player_default
import streamcoretv.feature.player.ui_web.generated.resources.web_player_error_title
import streamcoretv.feature.player.ui_web.generated.resources.web_player_fill
import streamcoretv.feature.player.ui_web.generated.resources.web_player_fit
import streamcoretv.feature.player.ui_web.generated.resources.web_player_off
import streamcoretv.feature.player.ui_web.generated.resources.web_player_quality
import streamcoretv.feature.player.ui_web.generated.resources.web_player_resize
import streamcoretv.feature.player.ui_web.generated.resources.web_player_retry
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings_back
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings_close
import streamcoretv.feature.player.ui_web.generated.resources.web_player_speed
import streamcoretv.feature.player.ui_web.generated.resources.web_player_subtitles

@Composable
internal fun WebPlayerSettingsOverlay(
    state: PlayerUiState,
    page: PlayerSettingsPage,
    onAction: (PlayerAction) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val rows = webPlayerSettingsRows(state = state, page = page)
    val rowKeys = remember(rows) { rows.map(WebPlayerSettingsRow::key) }
    val rowFocusRequesters = remember(rowKeys) {
        List(rowKeys.size) { FocusRequester() }
    }
    var previousPage by remember { mutableStateOf(page) }

    LaunchedEffect(page, rowKeys) {
        val targetIndex = if (page == PlayerSettingsPage.Root && previousPage != page) {
            rows.indexOfFirst { row -> row.page == previousPage }
        } else {
            0
        }.coerceAtLeast(0)
        val targetRequester = rowFocusRequesters.getOrNull(targetIndex) ?: backFocusRequester
        targetRequester.requestFocusWhenReady()
        previousPage = page
    }

    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(WebPlayerZOrder.Modal)
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup()
            .pointerInput(Unit) { detectTapGestures { } }
            .background(
                MaterialTheme.colorScheme.scrim.copy(
                    alpha = WebPlayerTokens.SettingsScrimAlpha,
                ),
            ),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = WebPlayerTokens.SettingsPanelAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .width(StreamCoreDimens.Tv.Player.SettingsPanelWidth)
                .fillMaxHeight()
                .testTag(PlayerTestTags.Settings),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = StreamCoreDimens.Tv.Player.SettingsSafeInset),
            ) {
                PlayerSettingsHeaderContent(
                    page = page,
                    titleStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.height(StreamCoreDimens.Tv.Player.SettingsHeaderHeight)
                        .padding(horizontal = StreamCoreDimens.Tv.Player.SettingsHorizontalPadding),
                    supportingStyle = MaterialTheme.typography.bodyMedium,
                    leadingContent = {
                        StreamCoreWebArtworkIconButton(
                            contentDescription = stringResource(
                                if (page == PlayerSettingsPage.Root) Res.string.web_player_settings_close else Res.string.web_player_settings_back,
                            ),
                            onClick = { onAction(PlayerAction.BackSelected) },
                            modifier = Modifier
                                .size(if (page == PlayerSettingsPage.Root) StreamCoreDimens.Tv.Player.SettingsIconContainerSize else StreamCoreDimens.Tv.Player.ControlSize)
                                .focusRequester(backFocusRequester)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                    left = FocusRequester.Cancel
                                    right = FocusRequester.Cancel
                                    down = rowFocusRequesters.firstOrNull() ?: FocusRequester.Cancel
                                }
                                .testTag(WebPlayerTestTags.SettingsBack),
                        ) {
                            if (page == PlayerSettingsPage.Root) {
                                PlayerSettingsIconContainer(
                                    icon = PlayerSettingsIconType.Settings,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    containerSize = StreamCoreDimens.Tv.Player.SettingsIconContainerSize,
                                    iconSize = StreamCoreDimens.Tv.Player.SettingsIconSize,
                                )
                            } else StreamCoreBackIcon()
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = WebPlayerTokens.SettingsDividerAlpha))
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = StreamCoreDimens.Tv.Player.SettingsHorizontalPadding,
                        end = StreamCoreDimens.Tv.Player.SettingsHorizontalPadding,
                        top = StreamCoreDimens.Spacing.Small,
                        bottom = StreamCoreDimens.Tv.Player.SettingsSafeInset,
                    ),
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    itemsIndexed(
                        items = rows,
                        key = { _, row -> row.key },
                        contentType = { _, row -> row.contentType },
                    ) { index, row ->
                        Column {
                            StreamCoreWebActionSurface(
                                onClick = { onAction(row.action) },
                                enabled = row.enabled,
                                role = if (row.page == null) Role.RadioButton else Role.Button,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (row.page == null) StreamCoreDimens.Tv.Player.SettingsSelectionRowHeight else StreamCoreDimens.Tv.Player.SettingsRowHeight)
                                    .focusRequester(rowFocusRequesters[index])
                                    .focusProperties {
                                        up = rowFocusRequesters.getOrNull(index - 1)
                                            ?: backFocusRequester
                                        down = rowFocusRequesters.getOrNull(index + 1)
                                            ?: FocusRequester.Cancel
                                        left = FocusRequester.Cancel
                                        right = FocusRequester.Cancel
                                    }
                                    .semantics {
                                        if (page != PlayerSettingsPage.Root) {
                                            selected = row.selected
                                            role = Role.RadioButton
                                        }
                                    }
                                    .testTag(PlayerTestTags.SettingsOptionPrefix + row.key),
                            ) {
                                if (row.page != null) {
                                    PlayerSettingsNavigationContent(
                                        title = row.label,
                                        value = row.value,
                                        icon = row.page.settingsIcon(),
                                        iconContainerSize = StreamCoreDimens.Tv.Player.SettingsIconContainerSize,
                                        iconSize = StreamCoreDimens.Tv.Player.SettingsIconSize,
                                        valueModifier = Modifier.widthIn(max = StreamCoreDimens.Tv.Player.SettingsValueMaxWidth),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = StreamCoreDimens.Spacing.Small),
                                    )
                                } else {
                                    Surface(
                                        color = if (row.selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = WebPlayerTokens.SettingsSelectedRowAlpha)
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                        contentColor = if (row.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxSize().padding(StreamCoreDimens.Tv.Focus.BorderWidth),
                                    ) {
                                        PlayerSettingsSelectionContent(
                                            label = row.label,
                                            selected = row.selected,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = StreamCoreDimens.Spacing.Large),
                                        )
                                    }
                                }
                            }
                            if (row.page != null) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = WebPlayerTokens.SettingsDividerAlpha),
                                    modifier = Modifier.padding(start = StreamCoreDimens.Spacing.Small + StreamCoreDimens.Tv.Player.SettingsIconContainerSize + StreamCoreDimens.Spacing.Large),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
internal fun WebPlayerErrorOverlay(
    message: String,
    recoverable: Boolean,
    onAction: (PlayerAction) -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(recoverable) {
        if (recoverable) {
            retryFocusRequester.requestFocusWhenReady()
        } else {
            backFocusRequester.requestFocusWhenReady()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(WebPlayerZOrder.Modal)
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup()
            .pointerInput(Unit) { detectTapGestures { } }
            .background(
                MaterialTheme.colorScheme.scrim.copy(
                    alpha = WebPlayerTokens.SettingsScrimAlpha,
                ),
            ),
    ) {
        StreamCoreWebPanel(
            modifier = Modifier
                .widthIn(max = WebPlayerTokens.ErrorPanelMaxWidth)
                .testTag(PlayerTestTags.Error),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            ) {
                Text(
                    text = stringResource(Res.string.web_player_error_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StreamCoreWebButton(
                        text = stringResource(Res.string.web_player_back),
                        onClick = { onAction(PlayerAction.BackSelected) },
                        variant = StreamCoreWebButtonVariant.Tertiary,
                        modifier = Modifier
                            .focusRequester(backFocusRequester)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = if (recoverable) {
                                    retryFocusRequester
                                } else {
                                    FocusRequester.Cancel
                                }
                            }
                            .testTag(WebPlayerTestTags.ErrorBack),
                    )
                    if (recoverable) {
                        StreamCoreWebButton(
                            text = stringResource(Res.string.web_player_retry),
                            onClick = { onAction(PlayerAction.Retry) },
                            modifier = Modifier
                                .focusRequester(retryFocusRequester)
                                .focusProperties {
                                    left = backFocusRequester
                                    right = FocusRequester.Cancel
                                }
                                .testTag(WebPlayerTestTags.ErrorRetry),
                        )
                    }
                }
            }
        }
    }

}

private data class WebPlayerSettingsRow(
    val key: String,
    val label: String,
    val value: String? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val page: PlayerSettingsPage? = null,
    val action: PlayerAction,
    val contentType: String,
)

@Composable
private fun webPlayerSettingsRows(
    state: PlayerUiState,
    page: PlayerSettingsPage,
): List<WebPlayerSettingsRow> {
    return when (page) {
        PlayerSettingsPage.Root -> listOf(
            rootSettingsRow(
                key = "quality",
                label = stringResource(Res.string.web_player_quality),
                value = selectedTrackLabel(state.videoTracks, state.selectedVideoTrackId)
                    ?: stringResource(Res.string.web_player_auto),
                page = PlayerSettingsPage.Quality,
            ),
            rootSettingsRow(
                key = "audio",
                label = stringResource(Res.string.web_player_audio),
                value = selectedTrackLabel(state.audioTracks, state.selectedAudioTrackId)
                    ?: stringResource(Res.string.web_player_default),
                page = PlayerSettingsPage.Audio,
            ),
            rootSettingsRow(
                key = "subtitles",
                label = stringResource(Res.string.web_player_subtitles),
                value = selectedTrackLabel(state.textTracks, state.selectedTextTrackId)
                    ?: stringResource(Res.string.web_player_off),
                page = PlayerSettingsPage.Subtitles,
            ),
            rootSettingsRow(
                key = "speed",
                label = stringResource(Res.string.web_player_speed),
                value = formatSpeed(state.speed),
                page = PlayerSettingsPage.Speed,
            ),
            rootSettingsRow(
                key = "resize",
                label = stringResource(Res.string.web_player_resize),
                value = resizeLabel(state.resizeMode),
                page = PlayerSettingsPage.ResizeMode,
            ),
        )

        PlayerSettingsPage.Quality -> trackRows(
            tracks = state.videoTracks,
            selectedId = state.selectedVideoTrackId,
            automaticLabel = stringResource(Res.string.web_player_auto),
            action = { id -> PlayerAction.SelectVideoTrack(id) },
        )

        PlayerSettingsPage.Audio -> trackRows(
            tracks = state.audioTracks,
            selectedId = state.selectedAudioTrackId,
            automaticLabel = stringResource(Res.string.web_player_default),
            action = { id -> PlayerAction.SelectAudioTrack(id) },
        )

        PlayerSettingsPage.Subtitles -> trackRows(
            tracks = state.textTracks,
            selectedId = state.selectedTextTrackId,
            automaticLabel = stringResource(Res.string.web_player_off),
            action = { id -> PlayerAction.SelectTextTrack(id) },
        )

        PlayerSettingsPage.Speed -> PlayerSettingsSpeedOptions.map { (label, speed) ->
            WebPlayerSettingsRow(
                key = "speed:$speed",
                label = label,
                selected = state.speed == speed,
                action = PlayerAction.SelectSpeed(speed),
                contentType = SettingsSelectionContentType,
            )
        }

        PlayerSettingsPage.ResizeMode -> PlaybackResizeMode.entries.map { mode ->
            WebPlayerSettingsRow(
                key = "resize:${mode.name}",
                label = resizeLabel(mode),
                selected = state.resizeMode == mode,
                action = PlayerAction.SelectResizeMode(mode),
                contentType = SettingsSelectionContentType,
            )
        }
    }
}

private fun rootSettingsRow(
    key: String,
    label: String,
    value: String,
    page: PlayerSettingsPage,
): WebPlayerSettingsRow {
    return WebPlayerSettingsRow(
        key = key,
        label = label,
        value = value,
        page = page,
        action = PlayerAction.OpenSettings(page),
        contentType = SettingsNavigationContentType,
    )
}

private fun trackRows(
    tracks: List<PlaybackTrackModel>,
    selectedId: String?,
    automaticLabel: String,
    action: (String?) -> PlayerAction,
): List<WebPlayerSettingsRow> {
    return buildList {
        add(
            WebPlayerSettingsRow(
                key = AutomaticTrackKey,
                label = automaticLabel,
                selected = selectedId == null,
                action = action(null),
                contentType = SettingsSelectionContentType,
            ),
        )
        tracks.forEach { track ->
            add(
                WebPlayerSettingsRow(
                    key = "track:${track.id}",
                    label = track.label,
                    selected = track.id == selectedId,
                    enabled = track.isSupported,
                    action = action(track.id),
                    contentType = SettingsSelectionContentType,
                ),
            )
        }
    }
}

private fun PlayerSettingsPage.settingsIcon(): PlayerSettingsIconType {
    return when (this) {
        PlayerSettingsPage.Root -> PlayerSettingsIconType.Settings
        PlayerSettingsPage.Quality -> PlayerSettingsIconType.Quality
        PlayerSettingsPage.Audio -> PlayerSettingsIconType.Audio
        PlayerSettingsPage.Subtitles -> PlayerSettingsIconType.Subtitles
        PlayerSettingsPage.Speed -> PlayerSettingsIconType.Speed
        PlayerSettingsPage.ResizeMode -> PlayerSettingsIconType.ResizeMode
    }
}

@Composable
private fun resizeLabel(mode: PlaybackResizeMode): String {
    return when (mode) {
        PlaybackResizeMode.Fit -> stringResource(Res.string.web_player_fit)
        PlaybackResizeMode.Fill -> stringResource(Res.string.web_player_fill)
    }
}

private fun selectedTrackLabel(
    tracks: List<PlaybackTrackModel>,
    selectedId: String?,
): String? {
    return tracks.firstOrNull { track -> track.id == selectedId }?.label
}

private fun formatSpeed(speed: Float): String {
    val value = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${value}×"
}

private const val AutomaticTrackKey = "automatic"
private const val SettingsNavigationContentType = "settings-navigation"
private const val SettingsSelectionContentType = "settings-selection"

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerSettingsOverlayPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebPlayerSettingsOverlay(
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Settings),
            page = PlayerSettingsPage.Root,
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerErrorOverlayPreview() {
    val state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.RecoverableError)
    StreamCoreTheme(darkTheme = true) {
        WebPlayerErrorOverlay(
            message = state.error?.message.orEmpty(),
            recoverable = true,
            onAction = {},
        )
    }
}
