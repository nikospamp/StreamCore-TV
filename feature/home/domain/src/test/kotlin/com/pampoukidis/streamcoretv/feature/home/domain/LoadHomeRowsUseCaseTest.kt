package com.pampoukidis.streamcoretv.feature.home.domain

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadHomeRowsUseCaseTest {

    @Test
    fun `delegates profile-scoped content loading to repository`() {
        runTest {
            val rows = listOf(rowModel())
            val repository = FakeHomeRepository(AppResult.Success(rows))
            val subject = LoadHomeRowsUseCase(repository)

            val result = subject("profile-1")

            assertEquals("profile-1", repository.requestedProfileId)
            assertEquals(AppResult.Success(rows), result)
        }
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

        var requestedProfileId: String? = null
            private set

        override suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>> {
            requestedProfileId = profileId
            return result
        }
    }
}
