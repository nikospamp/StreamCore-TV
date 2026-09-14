package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_audio_supporting
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_audio_title
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_quality_supporting
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_quality_title
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_resize_mode_supporting
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_resize_mode_title
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_settings_supporting
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_settings_title
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_speed_supporting
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_speed_title
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_subtitles_supporting
import streamcoretv.feature.player.ui_common.generated.resources.player_settings_subtitles_title
import streamcoretv.feature.player.ui_common.generated.resources.Res

@Composable
fun PlayerSettingsHeaderContent(
    page: PlayerSettingsPage,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    supportingStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    leadingContent: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        leadingContent()
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = page.settingsTitle(), style = titleStyle,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = page.settingsSupportingText(), style = supportingStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PlayerSettingsPage.settingsTitle(): String {
    return when (this) {
        PlayerSettingsPage.Root -> stringResource(Res.string.player_settings_settings_title)
        PlayerSettingsPage.Quality -> stringResource(Res.string.player_settings_quality_title)
        PlayerSettingsPage.Audio -> stringResource(Res.string.player_settings_audio_title)
        PlayerSettingsPage.Subtitles -> stringResource(Res.string.player_settings_subtitles_title)
        PlayerSettingsPage.Speed -> stringResource(Res.string.player_settings_speed_title)
        PlayerSettingsPage.ResizeMode -> stringResource(Res.string.player_settings_resize_mode_title)
    }
}

@Composable
fun PlayerSettingsPage.settingsSupportingText(): String {
    return when (this) {
        PlayerSettingsPage.Root -> stringResource(Res.string.player_settings_settings_supporting)
        PlayerSettingsPage.Quality -> stringResource(Res.string.player_settings_quality_supporting)
        PlayerSettingsPage.Audio -> stringResource(Res.string.player_settings_audio_supporting)
        PlayerSettingsPage.Subtitles -> stringResource(Res.string.player_settings_subtitles_supporting)
        PlayerSettingsPage.Speed -> stringResource(Res.string.player_settings_speed_supporting)
        PlayerSettingsPage.ResizeMode -> stringResource(Res.string.player_settings_resize_mode_supporting)
    }
}

@Preview
@Composable
private fun PlayerSettingsHeaderContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        PlayerSettingsHeaderContent(PlayerSettingsPage.Root) {
            PlayerSettingsIconContainer(
                PlayerSettingsIconType.Settings, MaterialTheme.colorScheme.onSurface,
                StreamCoreDimens.Mobile.Player.SettingsIconContainerSize, StreamCoreDimens.Icon.Standard,
            )
        }
    }
}
