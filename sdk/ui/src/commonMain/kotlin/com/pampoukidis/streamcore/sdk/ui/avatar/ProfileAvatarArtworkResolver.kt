package com.pampoukidis.streamcore.sdk.ui.avatar

import org.jetbrains.compose.resources.DrawableResource

fun interface ProfileAvatarArtworkResolver {
    fun resolve(avatarId: String): DrawableResource?
}
