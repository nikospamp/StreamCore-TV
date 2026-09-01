package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ProfileMutationUseCasesTest {

    @Test
    fun `module resolves profile use cases with a repository contract`() {
        val repository = RecordingProfileRepository()
        val application = koinApplication {
            modules(
                module {
                    single<ProfileRepository> { repository }
                },
                profilesDomainModule,
            )
        }

        assertIs<CreateProfileUseCase>(application.koin.get<CreateProfileUseCase>())
        assertIs<DeleteProfileUseCase>(application.koin.get<DeleteProfileUseCase>())
        assertIs<LoadProfileEditorOptionsUseCase>(application.koin.get<LoadProfileEditorOptionsUseCase>())
        assertIs<LoadProfilesUseCase>(application.koin.get<LoadProfilesUseCase>())
        assertIs<SelectProfileUseCase>(application.koin.get<SelectProfileUseCase>())
        assertIs<UpdateProfileUseCase>(application.koin.get<UpdateProfileUseCase>())
        assertIs<ValidateProfileDraftUseCase>(application.koin.get<ValidateProfileDraftUseCase>())

        application.close()
    }

    @Test
    fun `create maps a trimmed draft to the repository contract`() {
        runTest {
            val repository = RecordingProfileRepository()
            val subject = CreateProfileUseCase(repository)

            subject(
                ProfileDraftModel(
                    displayName = "  Viewer  ",
                    avatarId = "avatar-1",
                    parentalLevelId = "parental-1",
                ),
            )

            assertEquals(
                CreateProfileModel(
                    displayName = "Viewer",
                    avatarId = "avatar-1",
                    parentalLevelId = "parental-1",
                ),
                repository.createdProfile,
            )
        }
    }

    @Test
    fun `edit maps profile identity and trimmed draft to the repository contract`() {
        runTest {
            val repository = RecordingProfileRepository()
            val subject = UpdateProfileUseCase(repository)

            subject(
                ProfileDraftModel(
                    profileId = "profile-1",
                    displayName = "  Viewer  ",
                    avatarId = "avatar-1",
                    parentalLevelId = "parental-1",
                ),
            )

            assertEquals(
                UpdateProfileModel(
                    profileId = "profile-1",
                    displayName = "Viewer",
                    avatarId = "avatar-1",
                    parentalLevelId = "parental-1",
                ),
                repository.updatedProfile,
            )
        }
    }

    @Test
    fun `edit without profile identity fails without repository mutation`() {
        runTest {
            val repository = RecordingProfileRepository()
            val subject = UpdateProfileUseCase(repository)

            val result = subject(
                ProfileDraftModel(
                    profileId = null,
                    displayName = "Viewer",
                    avatarId = "avatar-1",
                    parentalLevelId = "parental-1",
                ),
            )

            val failure = assertIs<AppResult.Failure>(result)
            assertIs<AppError.Unknown>(failure.error)
            assertEquals(null, repository.updatedProfile)
        }
    }

    private class RecordingProfileRepository : ProfileRepository {

        var createdProfile: CreateProfileModel? = null
            private set

        var updatedProfile: UpdateProfileModel? = null
            private set

        override suspend fun getProfiles(): AppResult<List<ProfileModel>> {
            return AppResult.Success(emptyList())
        }

        override suspend fun getProfileEditorOptions(): AppResult<ProfileEditorOptionsModel> {
            return AppResult.Failure(AppError.Unknown())
        }

        override suspend fun createProfile(profile: CreateProfileModel): AppResult<ProfileModel> {
            createdProfile = profile
            return AppResult.Failure(AppError.Unknown())
        }

        override suspend fun updateProfile(profile: UpdateProfileModel): AppResult<ProfileModel> {
            updatedProfile = profile
            return AppResult.Failure(AppError.Unknown())
        }

        override suspend fun deleteProfile(profileId: String): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override suspend fun selectProfile(profileId: String): AppResult<ProfileModel> {
            return AppResult.Failure(AppError.Unknown())
        }
    }
}
