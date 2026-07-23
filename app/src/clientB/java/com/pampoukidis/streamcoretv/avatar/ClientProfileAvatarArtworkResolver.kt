package com.pampoukidis.streamcoretv.avatar

import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.data.client.R

object ClientProfileAvatarArtworkResolver : ProfileAvatarArtworkResolver {
    private val artworkByAvatarId = mapOf(
        "client-b-avatar-01" to R.drawable.profile_avatar_01,
        "client-b-avatar-02" to R.drawable.profile_avatar_03,
        "client-b-avatar-03" to R.drawable.profile_avatar_04,
        "client-b-avatar-04" to R.drawable.profile_avatar_05,
        "client-b-avatar-05" to R.drawable.profile_avatar_06,
        "client-b-avatar-06" to R.drawable.profile_avatar_07,
        "client-b-avatar-07" to R.drawable.profile_avatar_08,
        "client-b-avatar-08" to R.drawable.profile_avatar_09,
        "client-b-avatar-09" to R.drawable.profile_avatar_10,
    )

    override fun resolve(avatarId: String): Int? {
        return artworkByAvatarId[avatarId]
    }
}
