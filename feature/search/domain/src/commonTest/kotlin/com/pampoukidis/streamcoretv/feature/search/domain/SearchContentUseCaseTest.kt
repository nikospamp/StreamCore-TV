package com.pampoukidis.streamcoretv.feature.search.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchContentUseCaseTest {

    @Test
    fun normalizesSearchableQueryBeforeDelegating() = runTest {
            val repository = FakeSearchRepository()
            val subject = SearchContentUseCase(repository)

            subject(profileId = "profile-1", query = "  Deep   Current  ")

            assertEquals("profile-1", repository.profileId)
            assertEquals("Deep Current", repository.query)
    }

    @Test
    fun shortQueryReturnsEmptySuccessWithoutCallingRepository() = runTest {
            val repository = FakeSearchRepository()
            val subject = SearchContentUseCase(repository)

            val result = subject(profileId = "profile-1", query = " d ")

            assertEquals(AppResult.Success(emptyList<ContentModel>()), result)
            assertNull(repository.query)
    }

    private class FakeSearchRepository : SearchRepository {
        var profileId: String? = null
        var query: String? = null

        override suspend fun search(
            profileId: String,
            query: String,
        ): AppResult<List<ContentModel>> {
            this.profileId = profileId
            this.query = query
            return AppResult.Success(emptyList())
        }

        override suspend fun loadTrending(profileId: String): AppResult<List<ContentModel>> {
            return AppResult.Success(emptyList())
        }
    }
}
