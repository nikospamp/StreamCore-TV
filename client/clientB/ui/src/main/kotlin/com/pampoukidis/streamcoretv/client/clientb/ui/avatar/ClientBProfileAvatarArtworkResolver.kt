package com.pampoukidis.streamcoretv.client.clientb.ui.avatar

import com.pampoukidis.streamcoretv.client.clientb.ui.R
import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import javax.inject.Inject

class ClientBProfileAvatarArtworkResolver @Inject constructor() : ProfileAvatarArtworkResolver {
    private val artworkByAvatarId = mapOf(
        "client-b-avatar-01" to R.drawable.clientb_profile_avatar_01,
        "client-b-avatar-02" to R.drawable.clientb_profile_avatar_02,
        "client-b-avatar-03" to R.drawable.clientb_profile_avatar_03,
        "client-b-avatar-04" to R.drawable.clientb_profile_avatar_04,
        "client-b-avatar-05" to R.drawable.clientb_profile_avatar_05,
        "client-b-avatar-06" to R.drawable.clientb_profile_avatar_06,
        "client-b-avatar-07" to R.drawable.clientb_profile_avatar_07,
        "client-b-avatar-08" to R.drawable.clientb_profile_avatar_08,
        "client-b-avatar-09" to R.drawable.clientb_profile_avatar_09,
    )

    internal val supportedAvatarIds: Set<String>
        get() = artworkByAvatarId.keys

    override fun resolve(avatarId: String): Int? {
        return artworkByAvatarId[avatarId]
    }
}
