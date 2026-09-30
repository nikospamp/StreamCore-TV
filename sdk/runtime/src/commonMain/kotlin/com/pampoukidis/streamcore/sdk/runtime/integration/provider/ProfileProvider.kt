package com.pampoukidis.streamcore.sdk.runtime.integration.provider

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Backend profile management and PIN verification. Runtime owns profile activation and PIN challenges. */
interface ProfileProvider {

    suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>>

    suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions>

    suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile>

    suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile>

    suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit>

    suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfile>
    /** Returns PinRejected for an incorrect PIN; never classify it as account-session rejection. */
    suspend fun verifyProfilePin(profileId: String, pin: String): StreamCoreResult<Unit>
}
