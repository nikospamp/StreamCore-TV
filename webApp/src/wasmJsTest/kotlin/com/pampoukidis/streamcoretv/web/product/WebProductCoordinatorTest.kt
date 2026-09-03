package com.pampoukidis.streamcoretv.web.product

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.client.tmdb.data.di.TMDB_AUTH_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileParentalLevelModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfig
import com.pampoukidis.streamcoretv.web.graph.webModules
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebProductCoordinatorTest {
    @Test
    fun unauthorizedEditorErrorClearsOnlyAuthAndScopedSelectionPreferences(): TestResult {
        return runTest {
            val fixture = coordinatorFixture()
            val profileSnapshotKey = stringPreferencesKey("profiles_json.account-scope")
            fixture.coordinator.profileSelected(profile("selected"))
            fixture.authStore.edit { preferences ->
                preferences[stringPreferencesKey("session_id")] = "opaque-fixture"
                preferences[intPreferencesKey("account_id")] = 7
                preferences[stringPreferencesKey("account_username")] = "fixture-user"
                preferences[profileSnapshotKey] = "profile-snapshot-sentinel"
            }

            fixture.coordinator.handleError(AppError.Unauthorized())

            val preferences = fixture.authStore.data.first()
            assertNull(preferences[stringPreferencesKey("session_id")])
            assertNull(preferences[intPreferencesKey("account_id")])
            assertNull(preferences[stringPreferencesKey("account_username")])
            assertNull(preferences[selectedProfileIdKey(fixture.accountId)])
            assertEquals("profile-snapshot-sentinel", preferences[profileSnapshotKey])
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            assertEquals("/login", kotlinx.browser.window.location.pathname)
            fixture.close()
        }
    }

    @Test
    fun runtimeInvalidationRemovesAuthAndSelectionInOneTransaction(): TestResult {
        return runTest {
            val authStore = ControllablePreferencesDataStore()
            val fixture = coordinatorFixture(authStoreOverride = authStore)
            fixture.coordinator.profileSelected(profile("selected"))
            authStore.edit { preferences ->
                preferences[stringPreferencesKey("session_id")] = "session"
                preferences[intPreferencesKey("account_id")] = 7
            }
            val updatesBeforeInvalidation = authStore.updateCount

            fixture.coordinator.handleError(AppError.SessionExpired())

            assertEquals(updatesBeforeInvalidation + 1, authStore.updateCount)
            assertNull(authStore.snapshot()[stringPreferencesKey("session_id")])
            assertNull(authStore.snapshot()[intPreferencesKey("account_id")])
            assertNull(authStore.snapshot()[selectedProfileIdKey(fixture.accountId)])
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun failedRuntimeInvalidationRollsBackAtomicallyAndRoutesFailClosed(): TestResult {
        return runTest {
            val authStore = ControllablePreferencesDataStore()
            val fixture = coordinatorFixture(authStoreOverride = authStore)
            val profileSnapshotKey = stringPreferencesKey("profiles_json.atomic-failure")
            fixture.coordinator.profileSelected(profile("selected"))
            authStore.edit { preferences ->
                preferences[stringPreferencesKey("session_id")] = "session"
                preferences[profileSnapshotKey] = "profile-snapshot-sentinel"
            }
            val updatesBeforeInvalidation = authStore.updateCount
            authStore.updateFailure = IllegalStateException("storage unavailable")

            fixture.coordinator.handleError(AppError.Authentication())

            assertEquals(updatesBeforeInvalidation + 1, authStore.updateCount)
            assertEquals("session", authStore.snapshot()[stringPreferencesKey("session_id")])
            assertEquals("selected", authStore.snapshot()[selectedProfileIdKey(fixture.accountId)])
            assertEquals("profile-snapshot-sentinel", authStore.snapshot()[profileSnapshotKey])
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.navigation.replace(WebRoute.AuthenticatedLanding)
            fixture.coordinator.sanitizeRoute(WebRoute.AuthenticatedLanding)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun cancelledRuntimeInvalidationPropagatesAfterRoutingFailClosed(): TestResult {
        return runTest {
            val cancellation = CancellationException("cancelled")
            val authStore = ControllablePreferencesDataStore()
            val fixture = coordinatorFixture(authStoreOverride = authStore)
            fixture.coordinator.profileSelected(profile("selected"))
            authStore.edit { preferences ->
                preferences[stringPreferencesKey("session_id")] = "session"
            }
            val updatesBeforeInvalidation = authStore.updateCount
            authStore.updateFailure = cancellation

            val thrown = assertFailsWith<CancellationException> {
                fixture.coordinator.handleError(AppError.Unauthorized())
            }

            assertEquals(cancellation, thrown)
            assertEquals(updatesBeforeInvalidation + 1, authStore.updateCount)
            assertEquals("session", authStore.snapshot()[stringPreferencesKey("session_id")])
            assertEquals("selected", authStore.snapshot()[selectedProfileIdKey(fixture.accountId)])
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun selectedProfilePreferenceIsAccountScopedWithoutEmbeddingAccountId(): TestResult {
        return runTest {
            val fixture = coordinatorFixture(accountId = "Aa")
            val secondCoordinator = fixture.coordinatorFor(accountId = "BB")

            fixture.coordinator.profileSelected(profile("first-profile"))
            secondCoordinator.profileSelected(profile("second-profile"))

            val preferences = fixture.authStore.data.first()
            val firstKey = selectedProfileIdKey("Aa")
            val secondKey = selectedProfileIdKey("BB")
            assertNotEquals(firstKey, secondKey)
            assertEquals("first-profile", preferences[firstKey])
            assertEquals("second-profile", preferences[secondKey])
            assertTrue(firstKey.name.contains("Aa").not())
            assertTrue(secondKey.name.contains("BB").not())
            fixture.close()
        }
    }

    @Test
    fun definitiveLoggedOutBootstrapClearsPersistedSelection(): TestResult {
        return runTest {
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedOut),
            )
            fixture.coordinator.profileSelected(profile("previous-session"))

            val initialization = fixture.coordinator.initialize()

            assertEquals(WebProductInitialization.Ready, initialization)
            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun thrownBootstrapFailureReturnsGenericErrorAndRoutesFailClosed(): TestResult {
        return runTest {
            val fixture = coordinatorFixture(
                bootstrapThrowable = IllegalStateException("sensitive storage detail"),
            )
            fixture.coordinator.profileSelected(profile("previous-session"))

            val initialization = fixture.coordinator.initialize()

            val error = (initialization as WebProductInitialization.ReadyWithError).error
            assertTrue(error is AppError.Unknown)
            assertEquals("bootstrapAuth", error.source?.operation)
            assertEquals("AUTH_BOOTSTRAP_FAILURE", error.source?.backendCode)
            assertNull(error.source?.backendMessage)
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun thrownBootstrapCancellationRoutesFailClosedBeforePropagation(): TestResult {
        return runTest {
            val cancellation = CancellationException("cancelled")
            val fixture = coordinatorFixture(bootstrapThrowable = cancellation)
            fixture.coordinator.profileSelected(profile("previous-session"))

            val thrown = assertFailsWith<CancellationException> {
                fixture.coordinator.initialize()
            }

            assertEquals(cancellation, thrown)
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun failedLoggedOutCleanupReturnsSafeErrorAndRoutesFailClosed(): TestResult {
        return runTest {
            val authStore = ControllablePreferencesDataStore()
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedOut),
                authStoreOverride = authStore,
            )
            fixture.coordinator.profileSelected(profile("previous-session"))
            val updatesBeforeCleanup = authStore.updateCount
            authStore.updateFailure = IllegalStateException("storage unavailable")

            val initialization = fixture.coordinator.initialize()

            val error = (initialization as WebProductInitialization.ReadyWithError).error
            assertTrue(error is AppError.Unknown)
            assertEquals("restoreSelectedProfile", error.source?.operation)
            assertEquals(updatesBeforeCleanup + 1, authStore.updateCount)
            assertEquals(
                "previous-session",
                authStore.snapshot()[selectedProfileIdKey(fixture.accountId)],
            )
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun successfulLoginClearsSelectionFromPreviousSession(): TestResult {
        return runTest {
            val fixture = coordinatorFixture()
            fixture.coordinator.profileSelected(profile("previous-session"))

            fixture.coordinator.loginSucceeded()

            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Profiles, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun invalidatingColdStartFailureClearsPersistedSelection(): TestResult {
        return runTest {
            val error = AppError.SessionExpired()
            val fixture = coordinatorFixture(bootstrapResult = AppResult.Failure(error))
            fixture.coordinator.profileSelected(profile("persisted"))

            val initialization = fixture.coordinator.initialize()

            assertEquals(WebProductInitialization.ReadyWithError(error), initialization)
            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun transientColdStartFailurePreservesPersistedSelection(): TestResult {
        return runTest {
            val error = AppError.Network()
            val fixture = coordinatorFixture(bootstrapResult = AppResult.Failure(error))
            fixture.coordinator.profileSelected(profile("persisted"))

            val initialization = fixture.coordinator.initialize()

            assertEquals(WebProductInitialization.ReadyWithError(error), initialization)
            assertEquals(
                "persisted",
                fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)],
            )
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun profileReadFailurePreservesSelectionAndRoutesToSafeProfilesScreen(): TestResult {
        return runTest {
            val error = AppError.Unknown()
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                profilesResult = AppResult.Failure(error),
            )
            fixture.coordinator.profileSelected(profile("persisted"))

            val initialization = fixture.coordinator.initialize()

            assertEquals(WebProductInitialization.ReadyWithError(error), initialization)
            assertEquals(
                "persisted",
                fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)],
            )
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Profiles, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun selectionStorageReadFailureReturnsErrorWithoutMutatingPersistence(): TestResult {
        return runTest {
            val authStore = ControllablePreferencesDataStore()
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                profilesResult = AppResult.Success(listOf(profile("persisted"))),
                authStoreOverride = authStore,
            )
            fixture.coordinator.profileSelected(profile("persisted"))
            fixture.navigation.replace(WebRoute.Diagnostic)
            val updatesBeforeRestore = authStore.updateCount
            authStore.readFailure = IllegalStateException("storage unavailable")

            val initialization = fixture.coordinator.initialize()

            val error = (initialization as WebProductInitialization.ReadyWithError).error
            assertTrue(error is AppError.Unknown)
            assertEquals("restoreSelectedProfile", error.source?.operation)
            assertEquals("PROFILE_SELECTION_STORAGE_FAILURE", error.source?.backendCode)
            assertEquals(updatesBeforeRestore, authStore.updateCount)
            assertEquals(
                "persisted",
                authStore.snapshot()[selectedProfileIdKey(fixture.accountId)],
            )
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Diagnostic, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun cancellationReadingSelectionStoragePropagatesWithoutMutation(): TestResult {
        return runTest {
            val cancellation = CancellationException("cancelled")
            val authStore = ControllablePreferencesDataStore()
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                profilesResult = AppResult.Success(listOf(profile("persisted"))),
                authStoreOverride = authStore,
            )
            fixture.coordinator.profileSelected(profile("persisted"))
            val updatesBeforeRestore = authStore.updateCount
            authStore.readFailure = cancellation

            val thrown = assertFailsWith<CancellationException> {
                fixture.coordinator.initialize()
            }

            assertEquals(cancellation, thrown)
            assertEquals(updatesBeforeRestore, authStore.updateCount)
            assertEquals(
                "persisted",
                authStore.snapshot()[selectedProfileIdKey(fixture.accountId)],
            )
            fixture.close()
        }
    }

    @Test
    fun successfulRestoreValidationClearsSelectionProvenStale(): TestResult {
        return runTest {
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                profilesResult = AppResult.Success(listOf(profile("other"))),
            )
            fixture.coordinator.profileSelected(profile("stale"))

            val initialization = fixture.coordinator.initialize()

            assertEquals(WebProductInitialization.Ready, initialization)
            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            assertNull(fixture.coordinator.selectedProfile)
            assertEquals(WebRoute.Profiles, fixture.navigation.route.value)
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

            assertNull(fixture.coordinator.selectedProfile)
            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            fixture.close()
        }
    }

    @Test
    fun repeatedChangeProfileSafelyRemovesAbsentSelectionFromRealWebStore(): TestResult {
        return runTest {
            val fixture = coordinatorFixture()

            fixture.coordinator.changeProfile()
            fixture.coordinator.changeProfile()

            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            fixture.close()
        }
    }

    @Test
    fun authenticatedReloadPreservesEveryProtectedBrowseRoute(): TestResult {
        return runTest {
            listOf<WebRoute>(
                WebRoute.Home,
                WebRoute.Search,
                WebRoute.Library,
                WebRoute.Details("603"),
                WebRoute.Player("603"),
            ).forEach { route ->
                val selected = profile("selected")
                val fixture = coordinatorFixture(
                    bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                    profilesResult = AppResult.Success(listOf(selected)),
                )
                fixture.coordinator.profileSelected(selected)
                fixture.navigation.replace(route)

                assertEquals(WebProductInitialization.Ready, fixture.coordinator.initialize())
                assertEquals(route, fixture.navigation.route.value)
                fixture.close()
            }
        }
    }

    @Test
    fun protectedRouteWithoutSelectedProfileCanonicalizesToProfiles(): TestResult {
        return runTest {
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                profilesResult = AppResult.Success(listOf(profile("available"))),
            )
            fixture.navigation.replace(WebRoute.Details("603"))

            assertEquals(WebProductInitialization.Ready, fixture.coordinator.initialize())
            assertEquals(WebRoute.Profiles, fixture.coordinator.canonicalRoute(WebRoute.Details("603")))
            assertEquals(WebRoute.Profiles, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun authenticatedLegacyLandingCanonicalizesToHome(): TestResult {
        return runTest {
            val selected = profile("selected")
            val fixture = coordinatorFixture(
                bootstrapResult = AppResult.Success(AuthStateModel.LoggedIn(account = null)),
                profilesResult = AppResult.Success(listOf(selected)),
            )
            fixture.coordinator.profileSelected(selected)
            fixture.navigation.replace(WebRoute.AuthenticatedLanding)

            assertEquals(WebProductInitialization.Ready, fixture.coordinator.initialize())
            assertEquals(WebRoute.Home, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun successfulLogoutClearsSelectionAndRoutesToLogin(): TestResult {
        return runTest {
            val fixture = coordinatorFixture(
                logoutResult = AppResult.Success(Unit),
            )
            fixture.coordinator.profileSelected(profile("selected"))

            assertNull(fixture.coordinator.logout())
            assertNull(fixture.coordinator.selectedProfile)
            assertNull(fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)])
            assertEquals(WebRoute.Login, fixture.navigation.route.value)
            fixture.close()
        }
    }

    @Test
    fun failedLogoutPreservesSelectedProfileAndRoute(): TestResult {
        return runTest {
            val error = AppError.Network()
            val fixture = coordinatorFixture(
                logoutResult = AppResult.Failure(error),
            )
            fixture.coordinator.profileSelected(profile("selected"))

            assertEquals(error, fixture.coordinator.logout())
            assertEquals("selected", fixture.coordinator.selectedProfile?.id)
            assertEquals(
                "selected",
                fixture.authStore.data.first()[selectedProfileIdKey(fixture.accountId)],
            )
            assertEquals(WebRoute.Home, fixture.navigation.route.value)
            fixture.close()
        }
    }
}

private fun coordinatorFixture(
    accountId: String = "fixture-account",
    bootstrapResult: AppResult<AuthStateModel> = AppResult.Success(AuthStateModel.LoggedOut),
    bootstrapThrowable: Throwable? = null,
    profilesResult: AppResult<List<ProfileModel>> = AppResult.Success(emptyList()),
    authStoreOverride: DataStore<Preferences>? = null,
    logoutResult: AppResult<Unit> = AppResult.Success(Unit),
): CoordinatorFixture {
    val application = koinApplication {
        modules(
            webModules(
                config = WebRuntimeConfig(
                    tmdbBaseUrl = "https://api.example.test/3/",
                    tmdbReadAccessToken = "browser-visible-fixture",
                    tmdbAccountId = accountId,
                ),
                useSessionStorage = true,
            ),
        )
    }
    val navigation = WebNavigationController()
    val authStore = authStoreOverride ?: application.koin.get<DataStore<Preferences>>(
        named(TMDB_AUTH_STORE_QUALIFIER),
    )
    val authenticateRepository = StubAuthenticateRepository(
        bootstrapResult = bootstrapResult,
        bootstrapThrowable = bootstrapThrowable,
        logoutResult = logoutResult,
    )
    val profileRepository = StubProfileRepository(profilesResult)
    val coordinator = WebProductCoordinator(
        authenticateRepository = authenticateRepository,
        profileRepository = profileRepository,
        authStore = authStore,
        accountId = accountId,
        navigation = navigation,
    )
    return CoordinatorFixture(
        application = application,
        navigation = navigation,
        coordinator = coordinator,
        authStore = authStore,
        accountId = accountId,
        authenticateRepository = authenticateRepository,
        profileRepository = profileRepository,
    )
}

private data class CoordinatorFixture(
    val application: org.koin.core.KoinApplication,
    val navigation: WebNavigationController,
    val coordinator: WebProductCoordinator,
    val authStore: DataStore<Preferences>,
    val accountId: String,
    val authenticateRepository: AuthenticateRepository,
    val profileRepository: ProfileRepository,
) {
    fun coordinatorFor(accountId: String): WebProductCoordinator {
        return WebProductCoordinator(
            authenticateRepository = authenticateRepository,
            profileRepository = profileRepository,
            authStore = authStore,
            accountId = accountId,
            navigation = navigation,
        )
    }

    fun close() {
        navigation.close()
        application.close()
    }
}

private class StubAuthenticateRepository(
    private val bootstrapResult: AppResult<AuthStateModel>,
    private val bootstrapThrowable: Throwable?,
    private val logoutResult: AppResult<Unit>,
) : AuthenticateRepository {
    override val authState: Flow<AuthStateModel> = MutableStateFlow(AuthStateModel.LoggedOut)

    override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
        bootstrapThrowable?.let { throwable -> throw throwable }
        return bootstrapResult
    }

    override suspend fun loginUser(identifier: String, password: String): AppResult<Unit> {
        error("Not used by coordinator tests")
    }

    override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> {
        error("Not used by coordinator tests")
    }

    override suspend fun logoutUser(): AppResult<Unit> {
        return logoutResult
    }

    override suspend fun forgotPassword(email: String, otp: String?): AppResult<Unit> {
        error("Not used by coordinator tests")
    }
}

private class StubProfileRepository(
    private val profilesResult: AppResult<List<ProfileModel>>,
) : ProfileRepository {
    override suspend fun getProfiles(): AppResult<List<ProfileModel>> {
        return profilesResult
    }

    override suspend fun getProfileEditorOptions(): AppResult<ProfileEditorOptionsModel> {
        error("Not used by coordinator tests")
    }

    override suspend fun createProfile(profile: CreateProfileModel): AppResult<ProfileModel> {
        error("Not used by coordinator tests")
    }

    override suspend fun updateProfile(profile: UpdateProfileModel): AppResult<ProfileModel> {
        error("Not used by coordinator tests")
    }

    override suspend fun deleteProfile(profileId: String): AppResult<Unit> {
        error("Not used by coordinator tests")
    }

    override suspend fun selectProfile(profileId: String): AppResult<ProfileModel> {
        error("Not used by coordinator tests")
    }
}

private class ControllablePreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)

    var readFailure: Throwable? = null
    var updateFailure: Throwable? = null
    var updateCount: Int = 0
        private set

    override val data: Flow<Preferences>
        get() = flow {
            readFailure?.let { throwable -> throw throwable }
            emit(state.value)
        }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        updateCount += 1
        val updated = transform(state.value)
        updateFailure?.let { throwable -> throw throwable }
        state.value = updated
        return updated
    }

    fun snapshot(): Preferences {
        return state.value
    }
}

private fun profile(id: String): ProfileModel {
    return ProfileModel(
        id = id,
        displayName = id,
        avatar = ProfileAvatarModel(id = "avatar", imageUrl = null),
        parentalLevel = ProfileParentalLevelModel(
            id = "all",
            label = "All maturity",
            rank = 100,
        ),
        canDelete = true,
        isKidsProfile = false,
    )
}
