package com.pampoukidis.streamcore.sdk.providers.clientb.ui.avatar

import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.Res
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_01
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_02
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_03
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_04
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_05
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_06
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_07
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_08
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources.clientb_profile_avatar_09
import com.pampoukidis.streamcore.sdk.ui.avatar.ProfileAvatarArtworkResolver
import org.jetbrains.compose.resources.DrawableResource

class ClientBProfileAvatarArtworkResolver constructor() : ProfileAvatarArtworkResolver {
    private val artworkByAvatarId = mapOf(
        "client-b-avatar-01" to Res.drawable.clientb_profile_avatar_01,
        "client-b-avatar-02" to Res.drawable.clientb_profile_avatar_02,
        "client-b-avatar-03" to Res.drawable.clientb_profile_avatar_03,
        "client-b-avatar-04" to Res.drawable.clientb_profile_avatar_04,
        "client-b-avatar-05" to Res.drawable.clientb_profile_avatar_05,
        "client-b-avatar-06" to Res.drawable.clientb_profile_avatar_06,
        "client-b-avatar-07" to Res.drawable.clientb_profile_avatar_07,
        "client-b-avatar-08" to Res.drawable.clientb_profile_avatar_08,
        "client-b-avatar-09" to Res.drawable.clientb_profile_avatar_09,
    )

    internal val supportedAvatarIds: Set<String>
        get() = artworkByAvatarId.keys

    override fun resolve(avatarId: String): DrawableResource? {
        return artworkByAvatarId[avatarId]
    }
}
