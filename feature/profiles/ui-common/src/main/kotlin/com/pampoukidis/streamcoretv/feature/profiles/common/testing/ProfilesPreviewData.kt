package com.pampoukidis.streamcoretv.feature.profiles.common.testing

import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarCatalog
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileParentalLevelModel

object ProfilesPreviewData {
    val avatars = ProfileAvatarCatalog.ids.map { avatarId ->
        ProfileAvatarModel(id = avatarId, imageUrl = null)
    }

    val parentalLevels = listOf(
        ProfileParentalLevelModel(id = "all", label = "All maturity", rank = 100, isKids = false),
        ProfileParentalLevelModel(id = "teen", label = "Teen", rank = 60, isKids = false),
        ProfileParentalLevelModel(id = "kids", label = "Kids", rank = 20, isKids = true),
    )

    val profiles = listOf(
        ProfileModel(
            id = "profile-1",
            displayName = "Nikos",
            avatar = avatars[0],
            parentalLevel = parentalLevels[0],
            canDelete = false,
            isKidsProfile = false,
        ),
        ProfileModel(
            id = "profile-2",
            displayName = "Maria",
            avatar = avatars[5],
            parentalLevel = parentalLevels[1],
            canDelete = true,
            isKidsProfile = false,
        ),
        ProfileModel(
            id = "profile-3",
            displayName = "Kids",
            avatar = avatars[12],
            parentalLevel = parentalLevels[2],
            canDelete = true,
            isKidsProfile = true,
        ),
    )

    val editorOptions = ProfileEditorOptionsModel(
        avatars = avatars,
        parentalLevels = parentalLevels,
    )
}
