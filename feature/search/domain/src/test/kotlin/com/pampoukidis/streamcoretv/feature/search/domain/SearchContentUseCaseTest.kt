package com.pampoukidis.streamcoretv.feature.search.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchContentUseCaseTest {

    @Test
    fun `normalizes searchable query before delegating`() {
        runTest {
            val repository = FakeSearchRepository()
            val subject = SearchContentUseCase(repository)

            subject(profileId = "profile-1", query = "  Deep   Current  ")

            assertEquals("profile-1", repository.profileId)
            assertEquals("Deep Current", repository.query)
        }
    }

    @Test
    fun `short query returns empty success without calling repository`() {
        runTest {
            val repository = FakeSearchRepository()
            val subject = SearchContentUseCase(repository)

            val result = subject(profileId = "profile-1", query = " d ")

            assertEquals(AppResult.Success(emptyList<ContentModel>()), result)
            assertNull(repository.query)
        }
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
