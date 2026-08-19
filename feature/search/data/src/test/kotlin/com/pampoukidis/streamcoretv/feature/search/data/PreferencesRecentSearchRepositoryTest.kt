package com.pampoukidis.streamcoretv.feature.search.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRecentSearchRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `queries are normalized deduplicated newest first and capped`() {
        runTest {
            val repository = repository("ordered")
            repository.add("profile-a", " First ")
            repository.add("profile-a", "Second")
            repository.add("profile-a", "first")
            repository.add("profile-a", "Third")
            repository.add("profile-a", "Fourth")
            repository.add("profile-a", "Fifth")
            repository.add("profile-a", "Sixth")

            assertEquals(
                listOf("Sixth", "Fifth", "Fourth", "Third", "first"),
                repository.observe("profile-a").first(),
            )
        }
    }

    @Test
    fun `profiles are isolated and remove and clear are scoped`() {
        runTest {
            val repository = repository("profiles")
            repository.add("profile-a", "Shared")
            repository.add("profile-a", "Only A")
            repository.add("profile-b", "Shared")

            repository.remove("profile-a", "shared")

            assertEquals(listOf("Only A"), repository.observe("profile-a").first())
            assertEquals(listOf("Shared"), repository.observe("profile-b").first())

            repository.clear("profile-b")

            assertEquals(emptyList<String>(), repository.observe("profile-b").first())
        }
    }

    private fun kotlinx.coroutines.test.TestScope.repository(
        name: String,
    ): PreferencesRecentSearchRepository {
        val file = File(temporaryFolder.newFolder(name), "search.preferences_pb")
        return PreferencesRecentSearchRepository(
            dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { file },
            ),
            json = Json { encodeDefaults = true },
        )
    }
}
