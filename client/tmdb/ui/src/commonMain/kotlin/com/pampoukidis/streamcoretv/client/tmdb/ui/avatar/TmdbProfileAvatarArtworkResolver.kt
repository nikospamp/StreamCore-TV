package com.pampoukidis.streamcoretv.client.tmdb.ui.avatar

import streamcoretv.client.tmdb.ui.generated.resources.Res
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_01
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_02
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_03
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_04
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_05
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_06
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_07
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_08
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_09
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_10
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_11
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_12
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_13
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_14
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_15
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_16
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_17
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_18
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_19
import streamcoretv.client.tmdb.ui.generated.resources.tmdb_profile_avatar_20
import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import org.jetbrains.compose.resources.DrawableResource

class TmdbProfileAvatarArtworkResolver constructor() : ProfileAvatarArtworkResolver {
    private val artworkByAvatarId = mapOf(
        "tmdb-avatar-01" to Res.drawable.tmdb_profile_avatar_01,
        "tmdb-avatar-02" to Res.drawable.tmdb_profile_avatar_02,
        "tmdb-avatar-03" to Res.drawable.tmdb_profile_avatar_03,
        "tmdb-avatar-04" to Res.drawable.tmdb_profile_avatar_04,
        "tmdb-avatar-05" to Res.drawable.tmdb_profile_avatar_05,
        "tmdb-avatar-06" to Res.drawable.tmdb_profile_avatar_06,
        "tmdb-avatar-07" to Res.drawable.tmdb_profile_avatar_07,
        "tmdb-avatar-08" to Res.drawable.tmdb_profile_avatar_08,
        "tmdb-avatar-09" to Res.drawable.tmdb_profile_avatar_09,
        "tmdb-avatar-10" to Res.drawable.tmdb_profile_avatar_10,
        "tmdb-avatar-11" to Res.drawable.tmdb_profile_avatar_11,
        "tmdb-avatar-12" to Res.drawable.tmdb_profile_avatar_12,
        "tmdb-avatar-13" to Res.drawable.tmdb_profile_avatar_13,
        "tmdb-avatar-14" to Res.drawable.tmdb_profile_avatar_14,
        "tmdb-avatar-15" to Res.drawable.tmdb_profile_avatar_15,
        "tmdb-avatar-16" to Res.drawable.tmdb_profile_avatar_16,
        "tmdb-avatar-17" to Res.drawable.tmdb_profile_avatar_17,
        "tmdb-avatar-18" to Res.drawable.tmdb_profile_avatar_18,
        "tmdb-avatar-19" to Res.drawable.tmdb_profile_avatar_19,
        "tmdb-avatar-20" to Res.drawable.tmdb_profile_avatar_20,
    )

    internal val supportedAvatarIds: Set<String>
        get() = artworkByAvatarId.keys

    override fun resolve(avatarId: String): DrawableResource? {
        return artworkByAvatarId[avatarId]
    }
}
