package com.pampoukidis.streamcoretv.data.client.profile

import com.pampoukidis.streamcoretv.data.client.model.ProfileAvatarDto

internal object ClientBProfileAvatarCatalog {
    val avatars: List<ProfileAvatarDto> = (1..9).map { index ->
        ProfileAvatarDto(
            id = "client-b-avatar-${index.toString().padStart(2, '0')}",
            imageUrl = null,
        )
    }
}