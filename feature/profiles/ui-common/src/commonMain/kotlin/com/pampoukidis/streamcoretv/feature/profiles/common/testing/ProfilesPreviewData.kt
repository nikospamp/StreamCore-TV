package com.pampoukidis.streamcoretv.feature.profiles.common.testing

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileAvatar
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileParentalLevel

object ProfilesPreviewData {
    val avatars = (1..20).map { index ->
        StreamCoreProfileAvatar(
            id = "preview-avatar-${index.toString().padStart(2, '0')}",
            imageUrl = null,
        )
    }

    val parentalLevels = listOf(
        StreamCoreProfileParentalLevel(id = "all", label = "All maturity", rank = 100, isKids = false),
        StreamCoreProfileParentalLevel(id = "teen", label = "Teen", rank = 60, isKids = false),
        StreamCoreProfileParentalLevel(id = "kids", label = "Kids", rank = 20, isKids = true),
    )

    val profiles = listOf(
        StreamCoreProfile(
            id = "profile-1",
            displayName = "Nikos",
            avatar = avatars[0],
            parentalLevel = parentalLevels[0],
            canDelete = false,
            isKidsProfile = false,
        ),
        StreamCoreProfile(
            id = "profile-2",
            displayName = "Maria",
            avatar = avatars[5],
            parentalLevel = parentalLevels[1],
            canDelete = true,
            isKidsProfile = false,
        ),
        StreamCoreProfile(
            id = "profile-3",
            displayName = "Kids",
            avatar = avatars[12],
            parentalLevel = parentalLevels[2],
            canDelete = true,
            isKidsProfile = true,
        ),
    )

    val editorOptions = StreamCoreProfileEditorOptions(
        avatars = avatars,
        parentalLevels = parentalLevels,
    )
}