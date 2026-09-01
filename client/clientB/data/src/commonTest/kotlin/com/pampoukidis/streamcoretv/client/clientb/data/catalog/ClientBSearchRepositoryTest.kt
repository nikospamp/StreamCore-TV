package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.client.clientb.data.profile.ClientBProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class ClientBSearchRepositoryTest {

    private val profileRepository = ClientBProfileRepository()
    private val subject = ClientBSearchRepository(
        catalogSource = ClientBCatalogSource(),
        profileRepository = profileRepository,
    )

    @Test
    fun `search normalizes query and matches exact title`() {
        runTest {
            val result = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "  City   Lights  ",
            ).successValue()

            assertEquals(listOf("City Lights"), result.map(ContentModel::title))
            assertNull(result.single().row)
        }
    }

    @Test
    fun `search matches title prefix then title containment`() {
        runTest {
            val prefixResult = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "city",
            ).successValue()
            val containmentResult = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "lights",
            ).successValue()

            assertEquals(listOf("City Lights"), prefixResult.map(ContentModel::title))
            assertEquals(listOf("City Lights"), containmentResult.map(ContentModel::title))
        }
    }

    @Test
    fun `search matches contributor and preserves catalog order for equal relevance`() {
        runTest {
            val result = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "Jordan Lee",
            ).successValue()

            assertEquals(
                listOf(
                    "The Last Archive",
                    "City Lights",
                    "Paper Rockets",
                    "Deep Current",
                ),
                result.map(ContentModel::title),
            )
        }
    }

    @Test
    fun `search matches category before falling back to description`() {
        runTest {
            val categoryResult = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "drama",
            ).successValue()
            val descriptionResult = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "archivist",
            ).successValue()

            assertEquals(listOf("City Lights"), categoryResult.map(ContentModel::title))
            assertEquals(listOf("The Last Archive"), descriptionResult.map(ContentModel::title))
        }
    }

    @Test
    fun `kids profile searches only maturity-filtered catalog`() {
        runTest {
            val kidsProfile = createKidsProfile()

            val result = subject.search(
                profileId = kidsProfile.id,
                query = "Jordan Lee",
            ).successValue()

            assertEquals(listOf("Paper Rockets", "Deep Current"), result.map(ContentModel::title))
            assertTrue(result.all { content -> content.pgRatingLevel <= 7 })
        }
    }

    @Test
    fun `trending follows profile ranking order and is capped`() {
        runTest {
            val result = subject.loadTrending(OWNER_PROFILE_ID).successValue()

            assertEquals(
                listOf(
                    "The Last Archive",
                    "City Lights",
                    "Paper Rockets",
                    "Deep Current",
                ),
                result.map(ContentModel::title),
            )
            assertTrue(result.size <= 6)
            assertTrue(result.all { content -> content.row == null })
        }
    }

    @Test
    fun `unknown profile returns profile not found failure`() {
        runTest {
            val result = subject.search(
                profileId = "missing-profile",
                query = "city",
            )

            assertEquals("PROFILE_NOT_FOUND", result.failureBackendCode())
        }
    }

    @Test
    fun `blank profile and short query return validation failures`() {
        runTest {
            val blankProfileResult = subject.loadTrending(" ")
            val shortQueryResult = subject.search(
                profileId = OWNER_PROFILE_ID,
                query = "a",
            )

            assertEquals("PROFILE_ID_REQUIRED", blankProfileResult.failureBackendCode())
            assertEquals("QUERY_TOO_SHORT", shortQueryResult.failureBackendCode())
        }
    }

    private suspend fun createKidsProfile(): ProfileModel {
        val options = profileRepository.getProfileEditorOptions()
            .successValue<ProfileEditorOptionsModel>()
        val kidsLevel = options.parentalLevels.first { level -> level.isKids }
        return profileRepository.createProfile(
            CreateProfileModel(
                displayName = "Kids",
                avatarId = options.avatars.first().id,
                parentalLevelId = kidsLevel.id,
            ),
        ).successValue()
    }

    private fun AppResult<*>.failureBackendCode(): String? {
        val failure = this as AppResult.Failure
        val error = failure.error as AppError.Unknown
        return error.source?.backendCode
    }

    private fun <T> AppResult<T>.successValue(): T {
        return (this as AppResult.Success).value
    }

    private companion object {
        const val OWNER_PROFILE_ID = "client-b-profile-owner"
    }
}
