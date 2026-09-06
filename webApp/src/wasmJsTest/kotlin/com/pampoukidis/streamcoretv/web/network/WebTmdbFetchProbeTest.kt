package com.pampoukidis.streamcoretv.web.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.di.TMDB_AUTH_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.client.tmdb.data.di.tmdbDataModule
import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.web.graph.WebGraphHandle
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WebTmdbFetchProbeTest {
    @Test
    fun persistedProfileLetsProbeReachSearchWithItsQueryAndHeader(): TestResult {
        return runTest {
            val fixture = Fixture()
            try {
                assertEquals(WebTmdbFetchProbeResult.Success, WebTmdbFetchProbe().run(fixture.graph))
                val request = fixture.engine.requestHistory.single { it.url.encodedPath == "/3/search/movie" }
                assertEquals("web-fetch-probe", request.url.parameters["query"])
                assertEquals("true", request.url.parameters["include_adult"])
                assertEquals("Bearer fixture-token", request.headers[HttpHeaders.Authorization])
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun probeUsesThePersistedKidsSettingOfItsResolvedProfile(): TestResult {
        return runTest {
            val fixture = Fixture()
            try {
                val profiles = fixture.graph.application.koin.get<ProfileRepository>()
                val profile = assertIs<AppResult.Success<List<ProfileModel>>>(profiles.getProfiles()).value.first()
                assertIs<AppResult.Success<*>>(
                    profiles.updateProfile(UpdateProfileModel(profile.id, "Child", profile.avatar.id, "kids")),
                )

                assertEquals(WebTmdbFetchProbeResult.Success, WebTmdbFetchProbe().run(fixture.graph))
                val request = fixture.engine.requestHistory.single { it.url.encodedPath == "/3/search/movie" }
                assertEquals("false", request.url.parameters["include_adult"])
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun profileLookupFailurePropagatesWithoutMakingRequests(): TestResult {
        return runTest {
            val fixture = Fixture(profilesResult = AppResult.Failure(AppError.Parsing()))
            try {
                assertEquals(
                    WebTmdbFetchProbeResult.Failure("parsing-error"),
                    WebTmdbFetchProbe().run(fixture.graph),
                )
                assertEquals(0, fixture.engine.requestHistory.size)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun emptyProfilesReturnAnExplicitFailureWithoutMakingRequests(): TestResult {
        return runTest {
            val fixture = Fixture(profilesResult = AppResult.Success(emptyList()))
            try {
                assertEquals(
                    WebTmdbFetchProbeResult.Failure("profile-not-found"),
                    WebTmdbFetchProbe().run(fixture.graph),
                )
                assertEquals(0, fixture.engine.requestHistory.size)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun searchFailureRetainsTheExistingProbeErrorMapping(): TestResult {
        return runTest {
            val fixture = Fixture(searchStatus = HttpStatusCode.ServiceUnavailable)
            try {
                assertEquals(
                    WebTmdbFetchProbeResult.Failure("server-error"),
                    WebTmdbFetchProbe().run(fixture.graph),
                )
                assertEquals(1, fixture.engine.requestHistory.count { it.url.encodedPath == "/3/search/movie" })
            } finally {
                fixture.close()
            }
        }
    }

    private class Fixture(
        profilesResult: AppResult<List<ProfileModel>>? = null,
        searchStatus: HttpStatusCode = HttpStatusCode.OK,
    ) : AutoCloseable {
        val engine = MockEngine { request ->
            val body = when (request.url.encodedPath) {
                "/3/configuration" -> """{"images":{"secure_base_url":"https://images.test/","poster_sizes":["w500"],"backdrop_sizes":["w780"],"profile_sizes":["w185"]}}"""
                "/3/genre/movie/list" -> """{"genres":[]}"""
                "/3/search/movie" -> """{"page":1,"results":[],"total_pages":1,"total_results":0}"""
                else -> error("Unexpected endpoint: ${request.url.encodedPath}")
            }
            respond(
                content = body,
                status = if (request.url.encodedPath == "/3/search/movie") searchStatus else HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        private val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json() }
            defaultRequest {
                // KtorTmdbApi supplies the /3 path segment itself.
                url("https://tmdb.test/")
                header(HttpHeaders.Authorization, "Bearer fixture-token")
            }
        }
        val graph = WebGraphHandle(
            application = koinApplication {
                modules(
                    tmdbDataModule,
                    module {
                        single<HttpClient> { client }
                        single<DataStore<Preferences>>(named(TMDB_AUTH_STORE_QUALIFIER)) { TestStore() }
                        single { TmdbRuntimeConfig("https://tmdb.test/", "fixture-token", "probe-test") }
                        if (profilesResult != null) {
                            single<ProfileRepository> {
                                object : ProfileRepository by get<TmdbProfileRepository>() {
                                    override suspend fun getProfiles(): AppResult<List<ProfileModel>> {
                                        return profilesResult
                                    }
                                }
                            }
                        }
                    },
                )
            },
            resolvedDefinitions = emptyList(),
            storageNames = emptyList(),
        )

        override fun close() {
            graph.close()
            client.close()
        }
    }

    private class TestStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            return transform(state.value).also { state.value = it }
        }
    }
}
