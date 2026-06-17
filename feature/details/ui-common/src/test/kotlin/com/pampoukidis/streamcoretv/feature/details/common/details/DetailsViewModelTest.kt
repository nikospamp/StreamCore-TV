package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import com.pampoukidis.streamcoretv.feature.details.domain.LoadDetailsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load populates content and ignores duplicate request`() {
        runTest {
            val content = contentModel("content-1")
            val recommendations = listOf(contentModel("content-2"))
            val repository = FakeDetailsRepository(
                detailsResult = AppResult.Success(content),
                recommendationsResult = AppResult.Success(recommendations),
            )
            val subject = detailsViewModel(repository)
            val request = DetailsRequest(profileId = "profile-1", contentId = "content-1")

            subject.onAction(DetailsAction.Load(request))
            subject.onAction(DetailsAction.Load(request))
            runCurrent()

            assertEquals(content, subject.uiState.value.content)
            assertEquals(recommendations, subject.uiState.value.recommendations)
            assertFalse(subject.uiState.value.isLoading)
            assertEquals(1, repository.detailsRequestCount)
        }
    }

    @Test
    fun `refresh repeats active request`() {
        runTest {
            val repository = FakeDetailsRepository(
                detailsResult = AppResult.Success(contentModel("content-1")),
                recommendationsResult = AppResult.Success(emptyList()),
            )
            val subject = detailsViewModel(repository)

            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = "content-1"),
                ),
            )
            runCurrent()
            subject.onAction(DetailsAction.Refresh)
            runCurrent()

            assertEquals(2, repository.detailsRequestCount)
        }
    }

    @Test
    fun `load failure emits error`() {
        runTest {
            val error = AppError.Network()
            val subject = detailsViewModel(
                FakeDetailsRepository(
                    detailsResult = AppResult.Failure(error),
                    recommendationsResult = AppResult.Success(emptyList()),
                ),
            )
            val effect = async { subject.effects.first() }
            runCurrent()

            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = "content-1"),
                ),
            )
            runCurrent()

            assertEquals(DetailsEffect.ShowError(error), effect.await())
            assertFalse(subject.uiState.value.isLoading)
        }
    }

    @Test
    fun `recommendation selection emits navigation effect`() {
        runTest {
            val recommendation = contentModel("content-2")
            val subject = detailsViewModel()

            subject.onAction(DetailsAction.RecommendationSelected(recommendation))

            assertEquals(
                DetailsEffect.RecommendationSelected(recommendation),
                subject.effects.first(),
            )
        }
    }

    private fun detailsViewModel(
        repository: DetailsRepository = FakeDetailsRepository(
            detailsResult = AppResult.Success(contentModel("content-1")),
            recommendationsResult = AppResult.Success(emptyList()),
        ),
    ): DetailsViewModel {
        return DetailsViewModel(
            loadDetails = LoadDetailsUseCase(repository),
        )
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

        var detailsRequestCount: Int = 0
            private set

        override suspend fun getDetails(
            profileId: String,
            contentId: String,
        ): AppResult<ContentModel> {
            detailsRequestCount += 1
            return detailsResult
        }

        override suspend fun getRecommendations(
            profileId: String,
            contentId: String,
        ): AppResult<List<ContentModel>> {
            return recommendationsResult
        }
    }
}
