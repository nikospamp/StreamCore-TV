package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCheckIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun PlayerSettingsSelectionContent(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = label, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            StreamCoreCheckIcon(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview
@Composable
private fun PlayerSettingsSelectionContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        PlayerSettingsSelectionContent("Auto", selected = true)
    }
}
