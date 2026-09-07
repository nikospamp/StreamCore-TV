package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.AndroidProfilesBackdrop

@Composable
internal fun MobileProfilesBackdrop(modifier: Modifier = Modifier) {
    AndroidProfilesBackdrop(modifier = modifier)
}

@Preview
@Composable
private fun MobileProfilesBackdropPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesBackdrop(modifier = Modifier.fillMaxSize())
    }
}
