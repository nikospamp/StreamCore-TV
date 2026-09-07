package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.TouchProfileTile
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

@Composable
internal fun MobileProfileTile(
    profile: ProfileModel,
    mode: ProfilesMode,
    isSelecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    TouchProfileTile(
        profile = profile,
        mode = mode,
        isSelecting = isSelecting,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        sharedElementScope = sharedElementScope,
    )
}

@Preview
@Composable
private fun MobileProfileTilePreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            MobileProfileTile(
                profile = ProfilesPreviewData.profiles.first(),
                mode = ProfilesMode.Selection,
                isSelecting = false,
                enabled = true,
                onClick = {},
            )
        }
    }
}
