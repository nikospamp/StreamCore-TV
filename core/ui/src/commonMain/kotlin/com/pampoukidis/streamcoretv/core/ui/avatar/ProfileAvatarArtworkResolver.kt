package com.pampoukidis.streamcoretv.core.ui.avatar

import org.jetbrains.compose.resources.DrawableResource

fun interface ProfileAvatarArtworkResolver {
    fun resolve(avatarId: String): DrawableResource?
}
