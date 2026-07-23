package com.pampoukidis.streamcoretv.client.clientb.data.profile

import com.pampoukidis.streamcoretv.client.clientb.data.model.ProfileAvatarDto

internal object ClientBProfileAvatarCatalog {
    val avatars: List<ProfileAvatarDto> = (1..9).map { index ->
        ProfileAvatarDto(
            id = "client-b-avatar-${index.toString().padStart(2, '0')}",
            imageUrl = null,
        )
    }
}