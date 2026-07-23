package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileAvatarDto

internal object TmdbProfileAvatarCatalog {
    val avatars: List<ProfileAvatarDto> = (1..20).map { index ->
        ProfileAvatarDto(
            id = "tmdb-avatar-${index.toString().padStart(2, '0')}",
            imageUrl = null,
        )
    }
}