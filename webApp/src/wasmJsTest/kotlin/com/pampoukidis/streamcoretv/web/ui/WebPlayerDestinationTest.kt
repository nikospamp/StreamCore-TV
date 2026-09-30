package com.pampoukidis.streamcoretv.web.ui

import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
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
                StreamCoreResult.Success(content)
            }

            val resolution = resolveWebPlaybackRequest(
                profileId = ProfileId,
                contentId = ContentId,
                detailsRepository = repository,
            )

            val ready = assertIs<WebPlayerResolution.Ready>(resolution)
            assertEquals(
                StreamCorePlaybackRequest(
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
                StreamCoreResult.Failure(StreamCoreError.Network())
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

    private fun contentModel(): StreamCoreContent {
        return StreamCoreContent(
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
        private val detailsResult: suspend () -> StreamCoreResult<StreamCoreContent>,
    ) : DetailsService {
        var requestedProfileId: String? = null
            private set
        var requestedContentId: String? = null
            private set
        var recommendationRequestCount: Int = 0
            private set

        override suspend fun getDetails(
            profileId: String,
            contentId: String,
        ): StreamCoreResult<StreamCoreContent> {
            requestedProfileId = profileId
            requestedContentId = contentId
            return detailsResult()
        }

        override suspend fun getRecommendations(
            profileId: String,
            contentId: String,
        ): StreamCoreResult<List<StreamCoreContent>> {
            recommendationRequestCount += 1
            return StreamCoreResult.Success(emptyList())
        }
    }

    private companion object {
        const val ProfileId = "profile-42"
        const val ContentId = "content-603"
    }
}
