package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.domain.CreateProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.DeleteProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfileEditorOptionsUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfilesUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.UpdateProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.ValidateProfileDraftUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditorViewModelDeleteTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
        val error = AppError.Network()
        val repository = FakeProfileRepository(
            profiles = listOf(profile),
            deleteResult = AppResult.Failure(error),
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

    private fun load(subject: ProfileEditorViewModel, profileId: String) {
        subject.onAction(
            ProfileEditorAction.Load(
                mode = ProfileEditorMode.Edit,
                profileId = profileId,
            ),
        )
        mainDispatcherRule.scheduler.advanceUntilIdle()
    }

    private fun subject(repository: ProfileRepository): ProfileEditorViewModel {
        return ProfileEditorViewModel(
            loadProfiles = LoadProfilesUseCase(repository),
            loadProfileEditorOptions = LoadProfileEditorOptionsUseCase(repository),
            validateProfileDraft = ValidateProfileDraftUseCase(),
            createProfile = CreateProfileUseCase(repository),
            updateProfile = UpdateProfileUseCase(repository),
            deleteProfile = DeleteProfileUseCase(repository),
        )
    }

    private class FakeProfileRepository(
        private val profiles: List<ProfileModel>,
        private val deleteResult: AppResult<Unit> = AppResult.Success(Unit),
    ) : ProfileRepository {
        val deletedProfileIds = mutableListOf<String>()

        override suspend fun getProfiles(): AppResult<List<ProfileModel>> {
            return AppResult.Success(profiles)
        }

        override suspend fun getProfileEditorOptions(): AppResult<ProfileEditorOptionsModel> {
            return AppResult.Success(ProfilesPreviewData.editorOptions)
        }

        override suspend fun createProfile(profile: CreateProfileModel): AppResult<ProfileModel> {
            return AppResult.Failure(AppError.Unknown())
        }

        override suspend fun updateProfile(profile: UpdateProfileModel): AppResult<ProfileModel> {
            return AppResult.Failure(AppError.Unknown())
        }

        override suspend fun deleteProfile(profileId: String): AppResult<Unit> {
            deletedProfileIds += profileId
            return deleteResult
        }

        override suspend fun selectProfile(profileId: String): AppResult<ProfileModel> {
            return AppResult.Failure(AppError.Unknown())
        }
    }

    class MainDispatcherRule(
        val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
        val dispatcher: TestDispatcher = StandardTestDispatcher(scheduler),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
