package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.TouchAddProfileTile

@Composable
internal fun MobileAddProfileTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TouchAddProfileTile(enabled = enabled, onClick = onClick, modifier = modifier)
}

@Preview
@Composable
private fun MobileAddProfileTilePreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            MobileAddProfileTile(enabled = true, onClick = {})
        }
    }
}
