package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.client.clientb.data.profile.ClientBProfileRepository
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
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

            assertEquals(listOf("City Lights"), result.map(StreamCoreContent::title))
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

            assertEquals(listOf("City Lights"), prefixResult.map(StreamCoreContent::title))
            assertEquals(listOf("City Lights"), containmentResult.map(StreamCoreContent::title))
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
                result.map(StreamCoreContent::title),
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

            assertEquals(listOf("City Lights"), categoryResult.map(StreamCoreContent::title))
            assertEquals(listOf("The Last Archive"), descriptionResult.map(StreamCoreContent::title))
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

            assertEquals(listOf("Paper Rockets", "Deep Current"), result.map(StreamCoreContent::title))
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
                result.map(StreamCoreContent::title),
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

    private suspend fun createKidsProfile(): StreamCoreProfile {
        val options = profileRepository.getProfileEditorOptions()
            .successValue<StreamCoreProfileEditorOptions>()
        val kidsLevel = options.parentalLevels.first { level -> level.isKids }
        return profileRepository.createProfile(
            StreamCoreCreateProfile(
                displayName = "Kids",
                avatarId = options.avatars.first().id,
                parentalLevelId = kidsLevel.id,
            ),
        ).successValue()
    }

    private fun StreamCoreResult<*>.failureBackendCode(): String? {
        val failure = this as StreamCoreResult.Failure
        val error = failure.error as StreamCoreError.Unknown
        return error.source?.backendCode
    }

    private fun <T> StreamCoreResult<T>.successValue(): T {
        return (this as StreamCoreResult.Success).value
    }

    private companion object {
        const val OWNER_PROFILE_ID = "client-b-profile-owner"
    }
}
