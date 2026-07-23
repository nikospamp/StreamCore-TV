package com.pampoukidis.streamcoretv.avatar

import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.data.tmdb.R

object ClientProfileAvatarArtworkResolver : ProfileAvatarArtworkResolver {
    private val artworkByAvatarId = mapOf(
        "tmdb-avatar-01" to R.drawable.profile_avatar_01,
        "tmdb-avatar-02" to R.drawable.profile_avatar_03,
        "tmdb-avatar-03" to R.drawable.profile_avatar_04,
        "tmdb-avatar-04" to R.drawable.profile_avatar_05,
        "tmdb-avatar-05" to R.drawable.profile_avatar_06,
        "tmdb-avatar-06" to R.drawable.profile_avatar_07,
        "tmdb-avatar-07" to R.drawable.profile_avatar_08,
        "tmdb-avatar-08" to R.drawable.profile_avatar_09,
        "tmdb-avatar-09" to R.drawable.profile_avatar_10,
        "tmdb-avatar-10" to R.drawable.profile_avatar_11,
        "tmdb-avatar-11" to R.drawable.profile_avatar_12,
        "tmdb-avatar-12" to R.drawable.profile_avatar_13,
        "tmdb-avatar-13" to R.drawable.profile_avatar_14,
        "tmdb-avatar-14" to R.drawable.profile_avatar_15,
        "tmdb-avatar-15" to R.drawable.profile_avatar_16,
        "tmdb-avatar-16" to R.drawable.profile_avatar_17,
        "tmdb-avatar-17" to R.drawable.profile_avatar_18,
        "tmdb-avatar-18" to R.drawable.profile_avatar_19,
        "tmdb-avatar-19" to R.drawable.profile_avatar_20,
        "tmdb-avatar-20" to R.drawable.profile_avatar_21,
    )

    override fun resolve(avatarId: String): Int? {
        return artworkByAvatarId[avatarId]
    }
}
