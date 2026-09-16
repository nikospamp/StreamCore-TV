package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.playerSurface
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.painterResource
import streamcoretv.feature.player.ui_common.generated.resources.Res
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_pause_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_replay_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_replay_10_24dp
import streamcoretv.feature.player.ui_common.generated.resources.ic_forward_10_24dp
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_settings_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_fullscreen_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_fullscreen_exit_24
import streamcoretv.feature.player.ui_common.generated.resources.ic_player_pip_24

/** Portable artwork glyphs; platforms own focus, gestures, sizing, and action semantics. */
@Composable
fun PlayerControlIcon(
    icon: PlayerControlIconType,
    modifier: Modifier = Modifier,
) {
    if (icon == PlayerControlIconType.Play) {
        StreamCorePlayIcon(modifier = modifier.size(StreamCoreDimens.Icon.Standard))
        return
    }
    val resource = when (icon) {
        PlayerControlIconType.Pause -> Res.drawable.ic_player_pause_24
        PlayerControlIconType.Replay -> Res.drawable.ic_player_replay_24
        PlayerControlIconType.Rewind -> Res.drawable.ic_replay_10_24dp
        PlayerControlIconType.Forward -> Res.drawable.ic_forward_10_24dp
        PlayerControlIconType.Settings -> Res.drawable.ic_player_settings_24
        PlayerControlIconType.PictureInPicture -> Res.drawable.ic_player_pip_24
        PlayerControlIconType.Fullscreen -> Res.drawable.ic_player_fullscreen_24
        PlayerControlIconType.FullscreenExit -> Res.drawable.ic_player_fullscreen_exit_24
        PlayerControlIconType.Play -> error("Play is rendered by the shared core icon")
    }
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = modifier.size(StreamCoreDimens.Icon.Standard),
    )
}

@Preview
@Composable
private fun PlayerControlIconPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.playerSurface) {
            PlayerControlIcon(icon = PlayerControlIconType.Rewind)
        }
    }
}
