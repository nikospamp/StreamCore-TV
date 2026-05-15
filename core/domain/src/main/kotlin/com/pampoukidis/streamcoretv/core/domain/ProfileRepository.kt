package com.pampoukidis.streamcoretv.core.domain

import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

interface ProfileRepository {

    suspend fun getProfiles(): AppResult<List<ProfileModel>>

    suspend fun getProfileEditorOptions(): AppResult<ProfileEditorOptionsModel>

    suspend fun createProfile(profile: CreateProfileModel): AppResult<ProfileModel>

    suspend fun updateProfile(profile: UpdateProfileModel): AppResult<ProfileModel>

    suspend fun deleteProfile(profileId: String): AppResult<Unit>

    suspend fun selectProfile(profileId: String): AppResult<ProfileModel>
}
