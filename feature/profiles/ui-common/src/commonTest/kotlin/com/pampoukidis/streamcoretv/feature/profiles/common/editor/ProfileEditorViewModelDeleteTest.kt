package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileSelectionResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationIssue
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileFieldError
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditorViewModelDeleteTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUpDispatcher() {
        mainDispatcherRule.setUp()
    }

    @AfterTest
    fun tearDownDispatcher() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `successful deletion clears confirmation and emits deleted effect`() {
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        val repository = FakeProfileRepository(profiles = listOf(profile))
        val subject = subject(repository)
        load(subject = subject, profileId = profile.id)

        subject.onAction(ProfileEditorAction.RequestDeleteProfile)
        subject.onAction(ProfileEditorAction.ConfirmDeleteProfile)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        assertEquals(listOf(profile.id), repository.deletedProfileIds)
        assertFalse(subject.uiState.value.isSaving)
        assertNull(subject.uiState.value.pendingDeleteProfile)
        assertEquals(ProfileEditorEffect.ProfileDeleted, runBlocking { subject.effects.first() })
    }

    @Test
    fun `failed deletion preserves editor closes confirmation and emits error`() {
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        val error = StreamCoreError.Network()
        val repository = FakeProfileRepository(
            profiles = listOf(profile),
            deleteResult = StreamCoreResult.Failure(error),
        )
        val subject = subject(repository)
        load(subject = subject, profileId = profile.id)

        subject.onAction(ProfileEditorAction.RequestDeleteProfile)
        subject.onAction(ProfileEditorAction.ConfirmDeleteProfile)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        assertEquals(profile, subject.uiState.value.profile)
        assertNotNull(subject.uiState.value.editor)
        assertFalse(subject.uiState.value.isSaving)
        assertNull(subject.uiState.value.pendingDeleteProfile)
        assertEquals(ProfileEditorEffect.ShowError(error), runBlocking { subject.effects.first() })
    }

    @Test
    fun `protected profile ignores deletion request`() {
        val profile = ProfilesPreviewData.profiles.first { !it.canDelete }
        val repository = FakeProfileRepository(profiles = listOf(profile))
        val subject = subject(repository)
        load(subject = subject, profileId = profile.id)

        subject.onAction(ProfileEditorAction.RequestDeleteProfile)

        assertNull(subject.uiState.value.pendingDeleteProfile)
        assertEquals(emptyList<String>(), repository.deletedProfileIds)
    }

    @Test
    fun operationValidationMapsToExistingProfileFields() {
        val profile = ProfilesPreviewData.profiles.first()
        val repository = FakeProfileRepository(
            profiles = listOf(profile),
            updateResult = StreamCoreResult.Failure(StreamCoreError.Validation(listOf(
                StreamCoreValidationIssue(StreamCoreValidationField.ProfileName, StreamCoreValidationReason.TooLong),
                StreamCoreValidationIssue(StreamCoreValidationField.AvatarId, StreamCoreValidationReason.UnknownSelection),
                StreamCoreValidationIssue(StreamCoreValidationField.ParentalLevelId, StreamCoreValidationReason.Required),
            ))),
        )
        val subject = subject(repository)
        load(subject, profile.id)
        subject.onAction(ProfileEditorAction.DisplayNameChanged("Valid edited name"))
        subject.onAction(ProfileEditorAction.Submit)
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val validation = subject.uiState.value.editor!!.validation
        assertEquals(StreamCoreProfileFieldError.TooLong, validation.displayNameError)
        assertEquals(StreamCoreProfileFieldError.UnknownSelection, validation.avatarError)
        assertEquals(StreamCoreProfileFieldError.MissingSelection, validation.parentalLevelError)
        assertFalse(subject.uiState.value.isSaving)
    }

    private fun load(subject: ProfileEditorViewModel, profileId: String) {
        subject.onAction(
            ProfileEditorAction.Load(
                mode = ProfileEditorMode.Edit,
                profileId = profileId,
            ),
        )
        mainDispatcherRule.scheduler.advanceUntilIdle()
    }

    private fun subject(repository: ProfileService): ProfileEditorViewModel {
        return ProfileEditorViewModel(
            profileRepository = repository,
        )
    }

    private class FakeProfileRepository(
        private val profiles: List<StreamCoreProfile>,
        private val deleteResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit),
        private val updateResult: StreamCoreResult<StreamCoreProfile> = StreamCoreResult.Failure(StreamCoreError.Unknown()),
    ) : ProfileService {
        val deletedProfileIds = mutableListOf<String>()

        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> {
            return StreamCoreResult.Success(profiles)
        }

        override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> {
            return StreamCoreResult.Success(ProfilesPreviewData.editorOptions)
        }

        override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> {
            return StreamCoreResult.Failure(StreamCoreError.Unknown())
        }

        override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> {
            return updateResult
        }

        override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> {
            deletedProfileIds += profileId
            return deleteResult
        }

        override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult> {
            return StreamCoreResult.Failure(StreamCoreError.Unknown())
        }

        override suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult> { return StreamCoreResult.Failure(StreamCoreError.Unknown()) }
        override suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile> { return StreamCoreResult.Failure(StreamCoreError.Unknown()) }
        override fun cancelPin(challengeId: String): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun clearSelection(): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
    }

    class MainDispatcherRule(
        val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
        val dispatcher: TestDispatcher = StandardTestDispatcher(scheduler),
    ) {
        fun setUp() {
            Dispatchers.setMain(dispatcher)
        }

        fun tearDown() {
            Dispatchers.resetMain()
        }
    }
}
