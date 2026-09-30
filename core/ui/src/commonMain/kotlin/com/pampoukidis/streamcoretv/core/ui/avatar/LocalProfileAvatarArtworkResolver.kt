package com.pampoukidis.streamcoretv.core.ui.avatar

import com.pampoukidis.streamcore.sdk.ui.avatar.ProfileAvatarArtworkResolver
import androidx.compose.runtime.staticCompositionLocalOf

val LocalProfileAvatarArtworkResolver = staticCompositionLocalOf {
    ProfileAvatarArtworkResolver { null }
}
