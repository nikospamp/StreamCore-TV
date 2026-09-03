package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerFixtures
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerShowcaseScenario
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.player.ui_web.generated.resources.Res
import streamcoretv.feature.player.ui_web.generated.resources.web_player_audio
import streamcoretv.feature.player.ui_web.generated.resources.web_player_audio_hint
import streamcoretv.feature.player.ui_web.generated.resources.web_player_auto
import streamcoretv.feature.player.ui_web.generated.resources.web_player_back
import streamcoretv.feature.player.ui_web.generated.resources.web_player_default
import streamcoretv.feature.player.ui_web.generated.resources.web_player_error_title
import streamcoretv.feature.player.ui_web.generated.resources.web_player_fill
import streamcoretv.feature.player.ui_web.generated.resources.web_player_fit
import streamcoretv.feature.player.ui_web.generated.resources.web_player_off
import streamcoretv.feature.player.ui_web.generated.resources.web_player_quality
import streamcoretv.feature.player.ui_web.generated.resources.web_player_quality_hint
import streamcoretv.feature.player.ui_web.generated.resources.web_player_resize
import streamcoretv.feature.player.ui_web.generated.resources.web_player_resize_hint
import streamcoretv.feature.player.ui_web.generated.resources.web_player_retry
import streamcoretv.feature.player.ui_web.generated.resources.web_player_selected_value
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings_back
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings_close
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings_hint
import streamcoretv.feature.player.ui_web.generated.resources.web_player_settings_title
import streamcoretv.feature.player.ui_web.generated.resources.web_player_speed
import streamcoretv.feature.player.ui_web.generated.resources.web_player_speed_hint
import streamcoretv.feature.player.ui_web.generated.resources.web_player_subtitles
import streamcoretv.feature.player.ui_web.generated.resources.web_player_subtitles_hint

@Composable
internal fun WebPlayerSettingsDialog(
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

    Dialog(
        onDismissRequest = { onAction(PlayerAction.BackSelected) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxSize()
                .webEscape { onAction(PlayerAction.BackSelected) }
                .background(
                    MaterialTheme.colorScheme.scrim.copy(
                        alpha = WebPlayerTokens.SettingsScrimAlpha,
                    ),
                ),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = StreamCoreDimens.Elevation.Medium,
                modifier = Modifier
                    .width(WebPlayerTokens.SettingsPanelWidth)
                    .heightIn(max = WebPlayerTokens.SettingsPanelMaxHeight)
                    .fillMaxHeight()
                    .testTag(PlayerTestTags.Settings),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                    modifier = Modifier.padding(WebPlayerTokens.SettingsPanelPadding),
                ) {
                    StreamCoreWebButton(
                        text = if (page == PlayerSettingsPage.Root) {
                            stringResource(Res.string.web_player_settings_close)
                        } else {
                            stringResource(Res.string.web_player_settings_back)
                        },
                        onClick = { onAction(PlayerAction.BackSelected) },
                        variant = StreamCoreWebButtonVariant.Tertiary,
                        modifier = Modifier
                            .focusRequester(backFocusRequester)
                            .focusProperties {
                                up = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                                down = rowFocusRequesters.firstOrNull() ?: FocusRequester.Cancel
                            }
                            .testTag(WebPlayerTestTags.SettingsBack),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
                        Text(
                            text = settingsTitle(page),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = settingsHint(page),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        itemsIndexed(
                            items = rows,
                            key = { _, row -> row.key },
                            contentType = { _, row -> row.contentType },
                        ) { index, row ->
                            val label = if (row.value == null) {
                                row.label
                            } else {
                                stringResource(
                                    Res.string.web_player_selected_value,
                                    row.label,
                                    row.value,
                                )
                            }
                            StreamCoreWebButton(
                                text = label,
                                onClick = { onAction(row.action) },
                                enabled = row.enabled,
                                variant = if (row.selected) {
                                    StreamCoreWebButtonVariant.Primary
                                } else {
                                    StreamCoreWebButtonVariant.Secondary
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = WebPlayerTokens.SettingsItemMinHeight)
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
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WebPlayerErrorDialog(
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

    Dialog(
        onDismissRequest = { onAction(PlayerAction.BackSelected) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .webEscape { onAction(PlayerAction.BackSelected) }
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

        PlayerSettingsPage.Speed -> WebPlayerSpeedOptions.map { speed ->
            WebPlayerSettingsRow(
                key = "speed:$speed",
                label = formatSpeed(speed),
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

@Composable
private fun settingsTitle(page: PlayerSettingsPage): String {
    return when (page) {
        PlayerSettingsPage.Root -> stringResource(Res.string.web_player_settings_title)
        PlayerSettingsPage.Quality -> stringResource(Res.string.web_player_quality)
        PlayerSettingsPage.Audio -> stringResource(Res.string.web_player_audio)
        PlayerSettingsPage.Subtitles -> stringResource(Res.string.web_player_subtitles)
        PlayerSettingsPage.Speed -> stringResource(Res.string.web_player_speed)
        PlayerSettingsPage.ResizeMode -> stringResource(Res.string.web_player_resize)
    }
}

@Composable
private fun settingsHint(page: PlayerSettingsPage): String {
    return when (page) {
        PlayerSettingsPage.Root -> stringResource(Res.string.web_player_settings_hint)
        PlayerSettingsPage.Quality -> stringResource(Res.string.web_player_quality_hint)
        PlayerSettingsPage.Audio -> stringResource(Res.string.web_player_audio_hint)
        PlayerSettingsPage.Subtitles -> stringResource(Res.string.web_player_subtitles_hint)
        PlayerSettingsPage.Speed -> stringResource(Res.string.web_player_speed_hint)
        PlayerSettingsPage.ResizeMode -> stringResource(Res.string.web_player_resize_hint)
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
private fun WebPlayerSettingsDialogPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebPlayerSettingsDialog(
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Settings),
            page = PlayerSettingsPage.Root,
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebPlayerErrorDialogPreview() {
    val state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.RecoverableError)
    StreamCoreTheme(darkTheme = true) {
        WebPlayerErrorDialog(
            message = state.error?.message.orEmpty(),
            recoverable = true,
            onAction = {},
        )
    }
}
