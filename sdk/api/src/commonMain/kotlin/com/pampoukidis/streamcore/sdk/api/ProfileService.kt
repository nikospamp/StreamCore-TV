package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileSelectionResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile

/**
 * Account-owned profile management and runtime-enforced authorization. Listing/editor/CRUD operations require
 * an authenticated account; content services additionally require the active profile chosen through this service.
 * Suspend operations return [StreamCoreResult] for expected failures; coroutine cancellation still propagates.
 */
interface ProfileService {
    /**
     * Starts a fresh entry attempt, revoking previous profile authorization and any pending PIN challenge.
     * Returns no profiles, a chooser for multiple profiles, an activated singleton, or a singleton PIN challenge.
     * Use after login/fresh bootstrap, not on every chooser/editor refresh; [getProfiles] only refreshes the list.
     */
    suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult>
    /** Lists account profiles and reconciles a changed/deleted active profile with client context. Does not select. */
    suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>>
    /** Provider-owned avatar/maturity choices for account-level create/update validation. */
    suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions>
    /** Validates against current provider options and creates a profile without activating it. */
    suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile>
    /** Updates an owned profile and reconciles its current activation; changed PIN policy revokes access. */
    suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile>
    /** Enforces provider deletion permission and revokes any matching activation/challenge after success. */
    suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit>
    /**
     * Starts a new selection attempt, revoking the old activation. Success is either ready-to-use authorization
     * or a PIN challenge; selecting an owned ID alone does not bypass protection. Do not select again after Ready.
     */
    suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult>
    /**
     * Confirms the current opaque challenge with the provider-declared number of ASCII digits.
     * Success has already published activation in client.context. Wrong credentials return `PinRejected` without
     * logging out the account; absent capability returns `Unsupported`, and stale challenges return `InvalidContext`.
     * A selection-storage failure does not publish authorization. Call [cancelPin] when leaving the PIN flow;
     * cancelling the caller coroutine is not a substitute for explicitly abandoning a still-current challenge.
     */
    suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile>
    /**
     * Immediately invalidates the matching current challenge, including verification completing concurrently.
     * It does not log out the account. An already-expired challenge returns `InvalidContext`.
     */
    fun cancelPin(challengeId: String): StreamCoreResult<Unit>
    /**
     * Revokes in-memory access and pending challenges before clearing legacy saved selection.
     * A storage failure or cancellation during cleanup does not restore access. Reload the chooser with
     * [getProfiles] for explicit switching; reselecting a protected profile requires a new challenge.
     */
    suspend fun clearSelection(): StreamCoreResult<Unit>
}
