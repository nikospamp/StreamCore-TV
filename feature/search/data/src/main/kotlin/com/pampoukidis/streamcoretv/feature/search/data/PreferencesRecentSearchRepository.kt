package com.pampoukidis.streamcoretv.feature.search.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.feature.search.domain.RecentSearchRepository
import com.pampoukidis.streamcoretv.feature.search.domain.SearchQueryNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRecentSearchRepository @Inject constructor(
    @param:SearchHistoryStore private val dataStore: DataStore<Preferences>,
    @param:SearchHistoryJson private val json: Json,
) : RecentSearchRepository {

    private val preferences: Flow<RecentSearchPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { values -> decode(values[RecentSearchesKey]) }

    override fun observe(profileId: String): Flow<List<String>> {
        if (profileId.isBlank()) {
            return flowOf(emptyList())
        }

        return preferences.map { values ->
            values.queriesByProfile[profileId].orEmpty().take(MaxQueriesPerProfile)
        }
    }

    override suspend fun add(profileId: String, query: String) {
        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        if (profileId.isBlank() || normalizedQuery.isBlank()) {
            return
        }

        dataStore.edit { values ->
            val current = decode(values[RecentSearchesKey])
            val profileQueries = buildList {
                add(normalizedQuery)
                addAll(
                    current.queriesByProfile[profileId].orEmpty().filterNot { existing ->
                        existing.equals(normalizedQuery, ignoreCase = true)
                    },
                )
            }.take(MaxQueriesPerProfile)
            values[RecentSearchesKey] = encode(
                current.copy(
                    queriesByProfile = current.queriesByProfile + (profileId to profileQueries),
                ),
            )
        }
    }

    override suspend fun remove(profileId: String, query: String) {
        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        if (profileId.isBlank() || normalizedQuery.isBlank()) {
            return
        }

        dataStore.edit { values ->
            val current = decode(values[RecentSearchesKey])
            val retained = current.queriesByProfile[profileId].orEmpty().filterNot { existing ->
                existing.equals(normalizedQuery, ignoreCase = true)
            }
            values[RecentSearchesKey] = encode(
                current.copy(
                    queriesByProfile = current.queriesByProfile + (profileId to retained),
                ),
            )
        }
    }

    override suspend fun clear(profileId: String) {
        if (profileId.isBlank()) {
            return
        }

        dataStore.edit { values ->
            val current = decode(values[RecentSearchesKey])
            values[RecentSearchesKey] = encode(
                current.copy(queriesByProfile = current.queriesByProfile - profileId),
            )
        }
    }

    private fun decode(encoded: String?): RecentSearchPreferences {
        if (encoded.isNullOrBlank()) {
            return RecentSearchPreferences()
        }

        return runCatching {
            json.decodeFromString<RecentSearchPreferences>(encoded)
        }.getOrDefault(RecentSearchPreferences())
    }

    private fun encode(value: RecentSearchPreferences): String {
        return json.encodeToString(value)
    }

    private companion object {
        val RecentSearchesKey = stringPreferencesKey("recent_searches_json")
        const val MaxQueriesPerProfile = 5
    }
}
