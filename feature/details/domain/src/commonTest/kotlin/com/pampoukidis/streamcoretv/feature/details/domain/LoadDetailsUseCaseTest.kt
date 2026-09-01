package com.pampoukidis.streamcoretv.feature.details.domain

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class LoadDetailsUseCaseTest {

    @Test
    fun `module resolves details use case with a repository contract`() {
        val repository = FakeDetailsRepository(
            detailsResult = AppResult.Failure(AppError.Unknown()),
            recommendationsResult = AppResult.Success(emptyList()),
        )
        val application = koinApplication {
            modules(
                module {
                    single<DetailsRepository> { repository }
                },
                detailsDomainModule,
            )
        }

        assertIs<LoadDetailsUseCase>(application.koin.get<LoadDetailsUseCase>())

        application.close()
    }

    @Test
    fun `loads content and recommendations for request`() {
        runTest {
            val content = contentModel("content-1")
            val recommendations = listOf(contentModel("content-2"))
            val repository = FakeDetailsRepository(
                detailsResult = AppResult.Success(content),
                recommendationsResult = AppResult.Success(recommendations),
            )
            val subject = LoadDetailsUseCase(repository)

            val result = subject(DetailsRequest(profileId = "profile-1", contentId = "content-1"))

            val success = assertIs<AppResult.Success<*>>(result)
            val details = assertIs<com.pampoukidis.streamcoretv.feature.details.data.DetailsModel>(success.value)
            assertEquals(content, details.content)
            assertEquals(recommendations, details.recommendations)
            assertEquals("profile-1", repository.detailsProfileId)
            assertEquals("content-1", repository.detailsContentId)
            assertEquals("profile-1", repository.recommendationsProfileId)
            assertEquals("content-1", repository.recommendationsContentId)
        }
    }

    @Test
    fun `details failure does not request recommendations`() {
        runTest {
            val error = AppError.Network()
            val repository = FakeDetailsRepository(
                detailsResult = AppResult.Failure(error),
                recommendationsResult = AppResult.Success(emptyList()),
            )
            val subject = LoadDetailsUseCase(repository)

            val result = subject(DetailsRequest(profileId = "profile-1", contentId = "content-1"))

            assertEquals(AppResult.Failure(error), result)
            assertEquals(0, repository.recommendationsRequestCount)
        }
    }

    private fun contentModel(id: String): ContentModel {
        return ContentModel(
            id = id,
            title = "Content",
            description = "Description",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "poster",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        )
    }

    private class FakeDetailsRepository(
        private val detailsResult: AppResult<ContentModel>,
        private val recommendationsResult: AppResult<List<ContentModel>>,
    ) : DetailsRepository {

        var detailsProfileId: String? = null
            private set

        var detailsContentId: String? = null
            private set

        var recommendationsProfileId: String? = null
            private set

        var recommendationsContentId: String? = null
            private set

        var recommendationsRequestCount: Int = 0
            private set

        override suspend fun getDetails(
            profileId: String,
            contentId: String,
        ): AppResult<ContentModel> {
            detailsProfileId = profileId
            detailsContentId = contentId
            return detailsResult
        }

        override suspend fun getRecommendations(
            profileId: String,
            contentId: String,
        ): AppResult<List<ContentModel>> {
            recommendationsRequestCount += 1
            recommendationsProfileId = profileId
            recommendationsContentId = contentId
            return recommendationsResult
        }
    }
}
