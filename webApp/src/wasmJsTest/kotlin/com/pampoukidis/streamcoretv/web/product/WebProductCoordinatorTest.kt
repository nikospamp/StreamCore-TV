package com.pampoukidis.streamcoretv.web.product

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.client.tmdb.data.di.TMDB_AUTH_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfig
import com.pampoukidis.streamcoretv.web.graph.webModules
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebProductCoordinatorTest {
    @Test
    fun unauthorizedEditorErrorClearsActualAuthStoreAndReplacesHistory(): TestResult {
        return runTest {
            val fixture = coordinatorFixture()
            val authStore = fixture.application.koin.get<DataStore<Preferences>>(
                named(TMDB_AUTH_STORE_QUALIFIER),
            )
            authStore.edit { preferences ->
                preferences[stringPreferencesKey("session_id")] = "opaque-fixture"
                preferences[stringPreferencesKey("web_selected_profile_id")] = "tmdb-profile-owner"
            }

            fixture.coordinator.handleError(AppError.Unauthorized())

            assertTrue(authStore.data.first().asMap().isEmpty())
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            assertEquals("/login", kotlinx.browser.window.location.pathname)
            fixture.close()
        }
    }

    @Test
    fun deletingSelectedProfileViaProfilesHistoryClearsPersistedSelection(): TestResult {
        return runTest {
            val fixture = coordinatorFixture()
            val repository = fixture.application.koin.get<ProfileRepository>()
            val profiles = (repository.getProfiles() as AppResult.Success).value
            val selected = profiles.first { profile -> profile.canDelete }
            fixture.coordinator.profileSelected(selected)

            fixture.coordinator.reconcileProfiles(profiles.filterNot { profile -> profile.id == selected.id })

            val authStore = fixture.application.koin.get<DataStore<Preferences>>(
                named(TMDB_AUTH_STORE_QUALIFIER),
            )
            assertNull(fixture.coordinator.selectedProfile)
            assertNull(authStore.data.first()[stringPreferencesKey("web_selected_profile_id")])
            fixture.close()
        }
    }

    @Test
    fun repeatedChangeProfileSafelyRemovesAbsentSelectionFromRealWebStore(): TestResult {
        return runTest {
            val fixture = coordinatorFixture()

            fixture.coordinator.changeProfile()
            fixture.coordinator.changeProfile()

            val authStore = fixture.application.koin.get<DataStore<Preferences>>(
                named(TMDB_AUTH_STORE_QUALIFIER),
            )
            assertNull(authStore.data.first()[stringPreferencesKey("web_selected_profile_id")])
            fixture.close()
        }
    }
}

private fun coordinatorFixture(): CoordinatorFixture {
    val application = koinApplication {
        modules(
            webModules(
                config = WebRuntimeConfig(
                    tmdbBaseUrl = "https://api.example.test/3/",
                    tmdbReadAccessToken = "browser-visible-fixture",
                    tmdbAccountId = "42",
                ),
                useSessionStorage = true,
            ),
        )
    }
    val navigation = WebNavigationController()
    val coordinator = WebProductCoordinator(
        koin = application.koin,
        navigation = navigation,
    )
    return CoordinatorFixture(application, navigation, coordinator)
}

private data class CoordinatorFixture(
    val application: org.koin.core.KoinApplication,
    val navigation: WebNavigationController,
    val coordinator: WebProductCoordinator,
) {
    fun close() {
        navigation.close()
        application.close()
    }
}
