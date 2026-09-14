package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun PlayerSettingsNavigationContent(
    title: String,
    value: String?,
    icon: PlayerSettingsIconType,
    iconContainerSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    valueStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    valueModifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        PlayerSettingsIconContainer(icon, MaterialTheme.colorScheme.onSurfaceVariant, iconContainerSize, iconSize)
        Text(
            text = title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        value?.let {
            Text(
                text = it, style = valueStyle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = valueModifier,
            )
        }
        PlayerSettingsIcon(PlayerSettingsIconType.Chevron, Modifier.size(StreamCoreDimens.Icon.Medium))
    }
}

@Preview
@Composable
private fun PlayerSettingsNavigationContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        PlayerSettingsNavigationContent(
            "Quality", "Auto", PlayerSettingsIconType.Quality,
            StreamCoreDimens.Mobile.Player.SettingsIconContainerSize, StreamCoreDimens.Icon.Standard,
        )
    }
}
