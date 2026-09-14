package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun PlayerSettingsIconContainer(
    icon: PlayerSettingsIconType,
    tint: Color,
    containerSize: Dp,
    iconSize: Dp,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        contentColor = tint,
        modifier = Modifier.size(containerSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            PlayerSettingsIcon(icon, Modifier.size(iconSize), tint)
        }
    }
}

@Preview
@Composable
private fun PlayerSettingsIconContainerPreview() {
    StreamCoreTheme(darkTheme = true) {
        PlayerSettingsIconContainer(
            PlayerSettingsIconType.Settings, MaterialTheme.colorScheme.onSurface,
            StreamCoreDimens.Mobile.Player.SettingsIconContainerSize, StreamCoreDimens.Icon.Standard,
        )
    }
}
