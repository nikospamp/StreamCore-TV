package com.pampoukidis.streamcoretv.web.product

import com.pampoukidis.streamcore.sdk.api.AuthService
import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.api.HomeService
import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryNoProfiles
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryChooseProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryReady
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileSelectionResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileAvatar
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileParentalLevel
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Application routing tests consume only the public client.
 * Storage transactions, account isolation and rejected-session cleanup are SDK contract tests.
 */
class WebProductCoordinatorTest {
    @Test
    fun initialSessionRejectionIsPresentedOnceBeforeRetryingSdkSessionRestoration(): TestResult {
        return runTest {
            withFixture {
                val failure = StreamCoreResult.Failure(StreamCoreError.SessionExpired())
                client.contextState.value = StreamCoreContext(isAuthInitialized = true)
                navigation.replace(WebRoute.Home)
                val initialized = WebProductCoordinator(client, navigation, initialSessionRestorationResult = failure)
                assertEquals(WebProductInitialization.ReadyWithError(failure.error), initialized.initialize())
                assertEquals(WebRoute.Login, navigation.route.value)
                assertEquals(0, client.sessionRestorationCalls)
                assertEquals(WebProductInitialization.Ready, initialized.initialize())
                assertEquals(1, client.sessionRestorationCalls)
            }
        }
    }

    @Test
    fun profileCallbackRequiresSdkActivationAndNeverSelectsAgain(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("original"))
                navigation.replace(WebRoute.Profiles)
                assertIs<StreamCoreError.InvalidContext>(coordinator.profileSelected(profile("replacement")))
                assertEquals("original", coordinator.selectedProfile?.id)
                assertEquals(WebRoute.Profiles, navigation.route.value)
                assertEquals(0, client.selectionCalls)

                client.select(profile("replacement"))
                assertNull(coordinator.profileSelected(profile("replacement")))
                assertEquals(WebRoute.Home, navigation.route.value)
                assertEquals(0, client.selectionCalls)
            }
        }
    }
    @Test
    fun failedProfileSwitchPersistenceStillRevokesSelectionBeforeRetry(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                val failure = StreamCoreError.Storage()
                client.clearResult = StreamCoreResult.Failure(failure)

                assertEquals(failure, coordinator.changeProfile())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)

                client.clearResult = StreamCoreResult.Success(Unit)
                assertNull(coordinator.changeProfile())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }
    @Test
    fun cancelledProfileSwitchPropagatesAfterRevokingSelection(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                val cancellation = CancellationException("cancelled SDK operation")
                client.profileThrowable = cancellation

                assertEquals(cancellation, assertFailsWith<CancellationException> {
                    coordinator.changeProfile()
                })
                coordinator.synchronizeContext()
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }
    @Test
    fun loginSucceededUsesSdkAccountContextAndDoesNotWriteSelection(): TestResult {
        return runTest {
            withFixture {
                client.contextState.value = authenticatedContext()
                assertNull(coordinator.loginSucceeded())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
                assertEquals(0, client.clearCalls)

                client.contextState.value = StreamCoreContext(isAuthInitialized = true)
                coordinator.loginSucceeded()
                assertEquals(WebRoute.Login, navigation.route.value)
            }
        }
    }

    @Test
    fun staleSelectionCleanupFailureFinishesInitializationAtProfiles(): TestResult {
        return runTest {
            withFixture {
                val failure = StreamCoreError.Storage()
                client.contextState.value = authenticatedContext()
                client.sessionRestorationResult = StreamCoreResult.Failure(failure)
                navigation.replace(WebRoute.Home)

                assertEquals(WebProductInitialization.ReadyWithError(failure), coordinator.initialize())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }

    @Test
    fun deletedActiveProfileClosesProtectedRoutesFromSdkContext(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("deleted"))
                navigation.replace(WebRoute.Home)
                client.profilesResult = StreamCoreResult.Success(emptyList())

                assertNull(coordinator.reconcileProfilesFromRepository())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
                assertEquals(WebRoute.Profiles, coordinator.canonicalRoute(WebRoute.Home))
            }
        }
    }

    @Test
    fun sdkInvalidationDuringFailedLogoutRoutesToLoginAndPreservesPrimaryError(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                val failure = StreamCoreError.SessionExpired()
                client.logoutResult = StreamCoreResult.Failure(failure)
                client.logoutContext = StreamCoreContext(isAuthInitialized = true)

                assertEquals(failure, coordinator.logout())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Login, navigation.route.value)
                assertEquals(WebRoute.Login, coordinator.canonicalRoute(WebRoute.Home))
            }
        }
    }

    @Test
    fun failedLogoutPreservesSdkAuthenticatedSessionSelectionAndRoute(): TestResult {
        return runTest {
            for (error in listOf(StreamCoreError.Network(), StreamCoreError.Authentication(), StreamCoreError.Unauthorized(), StreamCoreError.Storage())) {
                withFixture {
                    client.select(profile("selected"))
                    navigation.replace(WebRoute.Home)
                    client.logoutResult = StreamCoreResult.Failure(error)

                    assertEquals(error, coordinator.logout())
                    assertEquals("selected", coordinator.selectedProfile?.id)
                    assertEquals(WebRoute.Home, navigation.route.value)
                    assertEquals(WebRoute.Home, coordinator.canonicalRoute(WebRoute.Home))
                }
            }
        }
    }

    @Test
    fun logoutExceptionIsSanitizedWhileCancellationPropagates(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                client.logoutThrowable = IllegalStateException("sensitive transport detail")

                val failure = assertIs<StreamCoreError.Unknown>(coordinator.logout())
                assertNull(failure.source?.backendMessage)
                assertEquals("selected", coordinator.selectedProfile?.id)
                assertEquals(WebRoute.Home, navigation.route.value)

                val cancellation = CancellationException("cancelled")
                client.logoutThrowable = cancellation
                assertEquals(cancellation, assertFailsWith<CancellationException> { coordinator.logout() })
                assertEquals(WebRoute.Home, navigation.route.value)
            }
        }
    }

    @Test
    fun successfulLogoutClearsSelectionAndRoutesToLogin(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                client.logoutContext = StreamCoreContext(isAuthInitialized = true)

                assertNull(coordinator.logout())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Login, navigation.route.value)
            }
        }
    }

    @Test
    fun authoritativeSdkContextChangeRoutesWithoutAnApplicationErrorCallback(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                client.contextState.value = StreamCoreContext(isAuthInitialized = true)

                coordinator.synchronizeContext()

                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Login, navigation.route.value)
                assertEquals(0, client.clearCalls)
            }
        }
    }

    @Test
    fun applicationErrorPresentationCannotInvalidateAnOtherwiseValidSdkSession(): TestResult {
        return runTest {
            for (error in listOf(StreamCoreError.Authentication(), StreamCoreError.Unauthorized(), StreamCoreError.SessionExpired())) {
                withFixture {
                    client.select(profile("selected"))
                    navigation.replace(WebRoute.Home)

                    coordinator.handleError(error)

                    assertEquals("selected", coordinator.selectedProfile?.id)
                    assertEquals(WebRoute.Home, navigation.route.value)
                    assertEquals(0, client.clearCalls)
                }
            }
        }
    }

    @Test
    fun switchingAccountCannotReuseAnApplicationCachedSelection(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("same-id"))
                navigation.replace(WebRoute.Home)
                client.contextState.value = authenticatedContext(accountId = "account-b")

                coordinator.synchronizeContext()

                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
                assertEquals("account-b", coordinator.context.value.account?.id)
            }
        }
    }

    @Test
    fun definitiveLoggedOutSessionRestorationRoutesToLoginWithoutApplicationCleanup(): TestResult {
        return runTest {
            withFixture {
                client.sessionRestorationResult = StreamCoreResult.Success(StreamCoreContext(isAuthInitialized = true))
                navigation.replace(WebRoute.Home)

                assertEquals(WebProductInitialization.Ready, coordinator.initialize())
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Login, navigation.route.value)
                assertEquals(0, client.clearCalls)
            }
        }
    }

    @Test
    fun thrownSessionRestorationFailureReturnsGenericErrorAndRoutesFailClosed(): TestResult {
        return runTest {
            withFixture {
                client.sessionRestorationThrowable = IllegalStateException("sensitive backend detail")
                navigation.replace(WebRoute.Home)

                val failure = assertIs<WebProductInitialization.ReadyWithError>(coordinator.initialize()).error
                assertIs<StreamCoreError.Unknown>(failure)
                assertNull(failure.source?.backendMessage)
                assertEquals(WebRoute.Login, navigation.route.value)
            }
        }
    }

    @Test
    fun thrownSessionRestorationCancellationRoutesFailClosedBeforePropagation(): TestResult {
        return runTest {
            withFixture {
                val cancellation = CancellationException("cancelled session restoration")
                client.sessionRestorationThrowable = cancellation
                navigation.replace(WebRoute.Home)

                assertEquals(cancellation, assertFailsWith<CancellationException> { coordinator.initialize() })
                assertEquals(WebRoute.Login, navigation.route.value)
            }
        }
    }

    @Test
    fun sdkSessionRestorationFailuresKeepTheirErrorAndUseValidatedContextForRouting(): TestResult {
        return runTest {
            for (error in listOf(StreamCoreError.SessionExpired(), StreamCoreError.Network(), StreamCoreError.Storage())) {
                withFixture {
                    client.contextState.value = StreamCoreContext()
                    client.sessionRestorationResult = StreamCoreResult.Failure(error)
                    navigation.replace(WebRoute.Home)

                    assertEquals(WebProductInitialization.ReadyWithError(error), coordinator.initialize())
                    assertEquals(WebRoute.Login, navigation.route.value)
                }
            }
        }
    }

    @Test
    fun profileReadFailurePreservesSdkSelection(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                val failure = StreamCoreError.Network()
                client.profilesResult = StreamCoreResult.Failure(failure)

                assertEquals(failure, coordinator.reconcileProfilesFromRepository())
                assertEquals("selected", coordinator.selectedProfile?.id)
                assertEquals(WebRoute.Home, navigation.route.value)
            }
        }
    }

    @Test
    fun profileEventsRenderReconciledSdkValuesInsteadOfTrustingEventPayload(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected").copy(displayName = "Updated in SDK"))
                navigation.replace(WebRoute.Profiles)

                assertNull(coordinator.reconcileProfiles(listOf(profile("selected"))))
                assertEquals("Updated in SDK", coordinator.selectedProfile?.displayName)
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }

    @Test
    fun repeatedChangeProfileSafelyRemovesAbsentSelection(): TestResult {
        return runTest {
            withFixture {
                assertNull(coordinator.changeProfile())
                assertNull(coordinator.changeProfile())
                assertEquals(2, client.clearCalls)
                assertNull(coordinator.selectedProfile)
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }

    @Test
    fun freshAuthenticatedReloadReentersProfilesFromEveryProtectedBrowseRoute(): TestResult {
        return runTest {
            val routes = listOf(WebRoute.Home, WebRoute.Search, WebRoute.Library, WebRoute.Details("film"), WebRoute.Player("film"))
            for (route in routes) {
                withFixture {
                    client.sessionRestorationResult = StreamCoreResult.Success(authenticatedContext())
                    navigation.replace(route)

                    assertEquals(WebProductInitialization.Ready, coordinator.initialize())
                    assertNull(coordinator.selectedProfile)
                    assertEquals(WebRoute.Profiles, navigation.route.value)
                }
            }
        }
    }

    @Test
    fun activeSessionPreservesProtectedBrowseRoutes(): TestResult {
        return runTest {
            val routes = listOf(WebRoute.Home, WebRoute.Search, WebRoute.Library, WebRoute.Details("film"), WebRoute.Player("film"))
            for (route in routes) {
                withFixture {
                    client.select(profile("selected"))
                    navigation.replace(route)
                    coordinator.synchronizeContext()
                    assertEquals(route, navigation.route.value)
                }
            }
        }
    }
    @Test
    fun protectedRouteWithoutSelectedProfileCanonicalizesToProfiles(): TestResult {
        return runTest {
            withFixture {
                client.sessionRestorationResult = StreamCoreResult.Success(authenticatedContext())
                navigation.replace(WebRoute.Home)

                assertEquals(WebProductInitialization.Ready, coordinator.initialize())
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }

    @Test
    fun restoredSessionLandingRequiresFreshProfileEntry(): TestResult {
        return runTest {
            withFixture {
                client.sessionRestorationResult = StreamCoreResult.Success(authenticatedContext())
                navigation.replace(WebRoute.AuthenticatedLanding)

                assertEquals(WebProductInitialization.Ready, coordinator.initialize())
                assertEquals(WebRoute.Profiles, navigation.route.value)
            }
        }
    }
    @Test
    fun closedSdkCannotKeepProtectedRoutesOpen(): TestResult {
        return runTest {
            withFixture {
                client.select(profile("selected"))
                navigation.replace(WebRoute.Home)
                client.close()

                coordinator.synchronizeContext()

                assertEquals(WebRoute.Login, navigation.route.value)
            }
        }
    }
}

private suspend fun withFixture(block: suspend CoordinatorFixture.() -> Unit) {
    val client = StubStreamCoreClient()
    val navigation = WebNavigationController()
    val fixture = CoordinatorFixture(client, navigation, WebProductCoordinator(client, navigation))
    try {
        fixture.block()
    } finally {
        navigation.close()
        client.close()
    }
}

private data class CoordinatorFixture(
    val client: StubStreamCoreClient,
    val navigation: WebNavigationController,
    val coordinator: WebProductCoordinator,
)

private class StubStreamCoreClient : StreamCoreClient {
    val contextState = MutableStateFlow(authenticatedContext())
    override val context: StateFlow<StreamCoreContext> = contextState
    override val configuration = StreamCoreConfiguration("test", "web-coordinator")
    override val capabilities = StreamCoreCapabilities()
    var sessionRestorationResult: StreamCoreResult<StreamCoreContext>? = null
    var sessionRestorationThrowable: Throwable? = null
    var sessionRestorationCalls = 0
        private set
    var selectionCalls = 0
        private set
    private var activationCount = 0
    var profilesResult: StreamCoreResult<List<StreamCoreProfile>> = StreamCoreResult.Success(emptyList())
    var clearResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit)
    var profileThrowable: Throwable? = null
    var logoutResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit)
    var logoutThrowable: Throwable? = null
    var logoutContext: StreamCoreContext? = null
    var clearCalls = 0
        private set

    fun select(profile: StreamCoreProfile) {
        contextState.value = contextState.value.copy(profile = profile, profileActivationId = "fixture-${++activationCount}")
    }

    override val auth: AuthService = object : AuthService {
        override suspend fun restoreSession(): StreamCoreResult<StreamCoreContext> {
            sessionRestorationCalls += 1
            sessionRestorationThrowable?.let { throw it }
            val result = sessionRestorationResult ?: StreamCoreResult.Success(contextState.value)
            if (result is StreamCoreResult.Success) contextState.value = result.value
            return result
        }

        override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> { error("Unused") }
        override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> { error("Unused") }
        override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> { error("Unused") }
        override suspend fun logout(): StreamCoreResult<Unit> {
            logoutThrowable?.let { throw it }
            logoutContext?.let { contextState.value = it }
            return logoutResult
        }
    }

    override val profiles: ProfileService = object : ProfileService {
        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> {
            val result = profilesResult
            if (result is StreamCoreResult.Success) {
                val selected = result.value.find { it.id == contextState.value.profile?.id }
                contextState.value = contextState.value.copy(
                    profile = selected,
                    profileActivationId = contextState.value.profileActivationId.takeIf { selected != null },
                )
            }
            return result
        }
        override suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult> {
            return when (val result = getProfiles()) {
                is StreamCoreResult.Failure -> result
                is StreamCoreResult.Success -> StreamCoreResult.Success(
                    if (result.value.isEmpty()) StreamCoreProfileEntryNoProfiles else StreamCoreProfileEntryChooseProfile(result.value),
                )
            }
        }
        override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult> {
            selectionCalls += 1
            val selected = profile(profileId)
            select(selected)
            return StreamCoreResult.Success(StreamCoreProfileEntryReady(selected))
        }
        override suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile> { error("Unused") }
        override fun cancelPin(challengeId: String): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun clearSelection(): StreamCoreResult<Unit> {
            clearCalls += 1
            contextState.value = contextState.value.copy(profile = null, profileActivationId = null)
            profileThrowable?.let { throw it }
            return clearResult
        }
        override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> { error("Unused") }
        override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> { error("Unused") }
        override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> { error("Unused") }
        override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> { error("Unused") }
    }

    override val home: HomeService get() = error("Unused")
    override val details: DetailsService get() = error("Unused")
    override val search: SearchService get() = error("Unused")
    override val library: LibraryService get() = error("Unused")
    override val playback: PlaybackService get() = error("Unused")

    override fun close() {
        contextState.value = contextState.value.copy(isClosed = true, account = null, profile = null, profileActivationId = null)
    }
}

private fun authenticatedContext(
    accountId: String = "fixture-account",
    profile: StreamCoreProfile? = null,
): StreamCoreContext {
    return StreamCoreContext(
        account = StreamCoreAuthAccount(accountId, "fixture", null),
        profile = profile,
        isAuthInitialized = true,
        profileActivationId = profile?.let { "fixture-${it.id}" },
    )
}

private fun profile(id: String): StreamCoreProfile {
    return StreamCoreProfile(
        id = id,
        displayName = id,
        avatar = StreamCoreProfileAvatar(id = "avatar", imageUrl = null),
        parentalLevel = StreamCoreProfileParentalLevel(id = "all", label = "All maturity", rank = 100),
        canDelete = true,
        isKidsProfile = false,
    )
}
