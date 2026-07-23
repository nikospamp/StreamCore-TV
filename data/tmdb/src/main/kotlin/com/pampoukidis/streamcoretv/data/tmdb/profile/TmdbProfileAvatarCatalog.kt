package com.pampoukidis.streamcoretv.data.tmdb.profile

import com.pampoukidis.streamcoretv.data.tmdb.model.ProfileAvatarDto

internal object TmdbProfileAvatarCatalog {
    val avatars: List<ProfileAvatarDto> = (1..20).map { index ->
        ProfileAvatarDto(
            id = "tmdb-avatar-${index.toString().padStart(2, '0')}",
            imageUrl = null,
        )
    }
}