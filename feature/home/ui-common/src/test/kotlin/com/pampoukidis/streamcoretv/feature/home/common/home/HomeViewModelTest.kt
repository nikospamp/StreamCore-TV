package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.home.domain.LoadHomeRowsUseCase
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load populates rows and ignores duplicate route load`() {
        runTest {
            val rows = listOf(rowModel())
            val repository = FakeHomeRepository(AppResult.Success(rows))
            val subject = homeViewModel(repository)

            subject.onAction(HomeAction.Load("profile-1"))
            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()

            assertEquals(rows, subject.uiState.value.rows)
            assertFalse(subject.uiState.value.isLoading)
            assertEquals(1, repository.requestCount)
        }
    }

    @Test
    fun `refresh repeats active profile request`() {
        runTest {
            val repository = FakeHomeRepository(AppResult.Success(listOf(rowModel())))
            val subject = homeViewModel(repository)

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()
            subject.onAction(HomeAction.Refresh)
            runCurrent()

            assertEquals(2, repository.requestCount)
            assertEquals("profile-1", repository.requestedProfileId)
        }
    }

    @Test
    fun `load failure emits error and stops loading`() {
        runTest {
            val error = AppError.Network()
            val subject = homeViewModel(FakeHomeRepository(AppResult.Failure(error)))
            val effect = async { subject.effects.first() }
            runCurrent()

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()

            assertEquals(HomeEffect.ShowError(error), effect.await())
            assertFalse(subject.uiState.value.isLoading)
        }
    }

    @Test
    fun `content selection emits selected content id`() {
        runTest {
            val content = contentModel()
            val subject = homeViewModel()

            subject.onAction(HomeAction.ContentSelected(content))

            assertEquals(HomeEffect.ContentSelected(content), subject.effects.first())
        }
    }

    private fun homeViewModel(
        repository: HomeRepository = FakeHomeRepository(AppResult.Success(emptyList())),
    ): HomeViewModel {
        return HomeViewModel(
            loadHomeRows = LoadHomeRowsUseCase(repository),
        )
    }

    private fun rowModel(): RowModel {
        return RowModel(
            id = "row-1",
            title = "Featured",
            subtitle = "Selected for you",
            content = listOf(contentModel()),
            type = RowType.Featured,
        )
    }

    private fun contentModel(): ContentModel {
        return ContentModel(
            id = "content-1",
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

    private class FakeHomeRepository(
        private val result: AppResult<List<RowModel>>,
    ) : HomeRepository {

        var requestCount: Int = 0
            private set

        var requestedProfileId: String? = null
            private set

        override suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>> {
            requestCount += 1
            requestedProfileId = profileId
            return result
        }
    }
}
