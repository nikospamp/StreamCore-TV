package com.pampoukidis.streamcoretv.feature.player.mobile.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCheckIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.mobile.R
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Spacing.ExtraLarge)
            .height(SettingsHeaderHeight),
    ) {
        if (page == PlayerSettingsPage.Root) {
            SettingsIconContainer(
                icon = R.drawable.ic_player_settings_24,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            StreamCoreArtworkIconButton(
                contentDescription = "Back to settings",
                onClick = onBack,
            ) {
                StreamCoreBackIcon()
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = page.title(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = page.supportingText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                    icon = R.drawable.ic_player_quality_24,
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
                    icon = R.drawable.ic_player_audio_24,
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
                    icon = R.drawable.ic_player_subtitles_24,
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
                icon = R.drawable.ic_player_speed_24,
                onClick = { onAction(PlayerAction.OpenSettings(PlayerSettingsPage.Speed)) },
            )
        }
        item(key = "resize-mode", contentType = "settings-navigation") {
            SettingsNavigationRow(
                title = "Resize mode",
                value = state.resizeMode.name,
                icon = R.drawable.ic_player_resize_mode_24,
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
    @DrawableRes icon: Int,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            SettingsIconContainer(
                icon = icon,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_player_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(StreamCoreDimens.Icon.Medium),
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SettingsDividerAlpha),
        modifier = Modifier.padding(start = SettingsDividerStartPadding),
    )
}

@Composable
private fun SettingsIconContainer(
    @DrawableRes icon: Int,
    tint: Color,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
            alpha = SettingsIconContainerAlpha,
        ),
        contentColor = tint,
        modifier = Modifier.size(SettingsIconContainerSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(SettingsIconSize),
            )
        }
    }
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
        options = PlaybackSpeedOptions,
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
        options = PlaybackResizeModeOptions,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(SettingsSelectionRowHeight)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(horizontal = StreamCoreDimens.Spacing.Large),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                StreamCoreCheckIcon(
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun PlayerSettingsPage?.dismissLabel(): String {
    return if (this == PlayerSettingsPage.Root) {
        "Dismiss playback settings"
    } else {
        "Back to settings"
    }
}

private fun PlayerSettingsPage.title(): String {
    return when (this) {
        PlayerSettingsPage.Root -> "Settings"
        PlayerSettingsPage.Quality -> "Quality"
        PlayerSettingsPage.Audio -> "Audio"
        PlayerSettingsPage.Subtitles -> "Subtitles"
        PlayerSettingsPage.Speed -> "Speed"
        PlayerSettingsPage.ResizeMode -> "Resize mode"
    }
}

private fun PlayerSettingsPage.supportingText(): String {
    return when (this) {
        PlayerSettingsPage.Root -> "Playback preferences"
        PlayerSettingsPage.Quality -> "Video resolution and data usage"
        PlayerSettingsPage.Audio -> "Spoken language"
        PlayerSettingsPage.Subtitles -> "Captions and language"
        PlayerSettingsPage.Speed -> "Playback rate"
        PlayerSettingsPage.ResizeMode -> "Picture fit"
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
private const val SettingsIconContainerAlpha = 0.72f
private const val SelectedRowAlpha = 0.72f
private val SettingsHeaderHeight = StreamCoreDimens.Mobile.Player.SettingsHeaderHeight
private val SettingsRowHeight = StreamCoreDimens.Mobile.Player.SettingsRowHeight
private val SettingsSelectionRowHeight = StreamCoreDimens.Mobile.Player.SettingsSelectionRowHeight
private val SettingsIconContainerSize = StreamCoreDimens.Mobile.Player.SettingsIconContainerSize
private val SettingsIconSize = StreamCoreDimens.Mobile.Player.SettingsIconSize
private val SettingsDividerStartPadding = StreamCoreDimens.Mobile.Player.SettingsDividerStartPadding
private val PlaybackSpeedOptions = listOf(
    "0.5×" to 0.5f,
    "0.75×" to 0.75f,
    "1.0×" to 1f,
    "1.25×" to 1.25f,
    "1.5×" to 1.5f,
    "2.0×" to 2f,
)
private val PlaybackResizeModeOptions = PlaybackResizeMode.entries.map { mode ->
    mode.name to mode
}

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
