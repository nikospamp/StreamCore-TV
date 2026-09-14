package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.painterResource
import streamcoretv.feature.player.ui_common.generated.resources.Res
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_settings_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_quality_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_audio_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_subtitles_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_speed_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_resize_mode_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_chevron_right_24

/** Portable settings artwork; input and focus remain in platform controls. */
@Composable
fun PlayerSettingsIcon(
    icon: PlayerSettingsIconType,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val resource = when (icon) {
        PlayerSettingsIconType.Settings -> Res.drawable.ic_player_settings_24
        PlayerSettingsIconType.Quality -> Res.drawable.ic_player_quality_24
        PlayerSettingsIconType.Audio -> Res.drawable.ic_player_audio_24
        PlayerSettingsIconType.Subtitles -> Res.drawable.ic_player_subtitles_24
        PlayerSettingsIconType.Speed -> Res.drawable.ic_player_speed_24
        PlayerSettingsIconType.ResizeMode -> Res.drawable.ic_player_resize_mode_24
        PlayerSettingsIconType.Chevron -> Res.drawable.ic_player_chevron_right_24
    }
    Icon(painterResource(resource), contentDescription = null, modifier = modifier, tint = tint)
}

@Preview
@Composable
private fun PlayerSettingsIconPreview() {
    StreamCoreTheme(darkTheme = true) {
        PlayerSettingsIcon(PlayerSettingsIconType.Settings, Modifier.size(StreamCoreDimens.Icon.Standard))
    }
}
