package com.pampoukidis.streamcore.sdk.providers.clientb.profile



internal object ClientBProfileAvatarCatalog {
    val avatars: List<ProfileAvatarDto> = (1..9).map { index ->
        ProfileAvatarDto(
            id = "client-b-avatar-${index.toString().padStart(2, '0')}",
            imageUrl = null,
        )
    }
}