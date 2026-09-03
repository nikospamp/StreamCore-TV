package com.pampoukidis.streamcoretv.web.ui

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

internal class WebPlayerDestinationTest {
    @Test
    fun successfulDetailsResolutionMapsTheExactPlaybackRequest(): TestResult {
        return runTest {
            val content = contentModel()
            val repository = FakeDetailsRepository {
                AppResult.Success(content)
            }

            val resolution = resolveWebPlaybackRequest(
                profileId = ProfileId,
                contentId = ContentId,
                detailsRepository = repository,
            )

            val ready = assertIs<WebPlayerResolution.Ready>(resolution)
            assertEquals(
                PlaybackRequestModel(
                    profileId = ProfileId,
                    contentId = ContentId,
                    contentSnapshot = content,
                ),
                ready.request,
            )
            assertEquals(ProfileId, repository.requestedProfileId)
            assertEquals(ContentId, repository.requestedContentId)
            assertEquals(0, repository.recommendationRequestCount)
        }
    }

    @Test
    fun detailsFailureReturnsUnavailable(): TestResult {
        return runTest {
            val repository = FakeDetailsRepository {
                AppResult.Failure(AppError.Network())
            }

            val resolution = resolveWebPlaybackRequest(
                profileId = ProfileId,
                contentId = ContentId,
                detailsRepository = repository,
            )

            assertEquals(WebPlayerResolution.Unavailable, resolution)
        }
    }

    @Test
    fun thrownNonCancellationReturnsUnavailable(): TestResult {
        return runTest {
            val repository = FakeDetailsRepository {
                throw IllegalStateException("repository failure")
            }

            val resolution = resolveWebPlaybackRequest(
                profileId = ProfileId,
                contentId = ContentId,
                detailsRepository = repository,
            )

            assertEquals(WebPlayerResolution.Unavailable, resolution)
        }
    }

    @Test
    fun cancellationIsRethrown(): TestResult {
        return runTest {
            val cancellation = CancellationException("cancel resolution")
            val repository = FakeDetailsRepository {
                throw cancellation
            }

            val thrown = assertFailsWith<CancellationException> {
                resolveWebPlaybackRequest(
                    profileId = ProfileId,
                    contentId = ContentId,
                    detailsRepository = repository,
                )
            }

            assertSame(cancellation, thrown)
        }
    }

    private fun contentModel(): ContentModel {
        return ContentModel(
            id = ContentId,
            title = "Backend-neutral title",
            description = "Backend-neutral description",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "poster-path",
            backdrop = "backdrop-path",
            cast = emptyList(),
            releaseDate = 1_700_000_000_000L,
            genres = emptyList(),
            row = "featured",
        )
    }

    private class FakeDetailsRepository(
        private val detailsResult: suspend () -> AppResult<ContentModel>,
    ) : DetailsRepository {
        var requestedProfileId: String? = null
            private set
        var requestedContentId: String? = null
            private set
        var recommendationRequestCount: Int = 0
            private set

        override suspend fun getDetails(
            profileId: String,
            contentId: String,
        ): AppResult<ContentModel> {
            requestedProfileId = profileId
            requestedContentId = contentId
            return detailsResult()
        }

        override suspend fun getRecommendations(
            profileId: String,
            contentId: String,
        ): AppResult<List<ContentModel>> {
            recommendationRequestCount += 1
            return AppResult.Success(emptyList())
        }
    }

    private companion object {
        const val ProfileId = "profile-42"
        const val ContentId = "content-603"
    }
}
