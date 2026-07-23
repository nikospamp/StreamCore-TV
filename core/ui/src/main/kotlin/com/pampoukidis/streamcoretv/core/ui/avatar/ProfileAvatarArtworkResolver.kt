package com.pampoukidis.streamcoretv.core.ui.avatar

import androidx.annotation.DrawableRes

fun interface ProfileAvatarArtworkResolver {
    @DrawableRes
    fun resolve(avatarId: String): Int?
}
