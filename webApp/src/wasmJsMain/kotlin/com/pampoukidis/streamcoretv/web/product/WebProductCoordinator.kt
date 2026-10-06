package com.pampoukidis.streamcoretv.web.product

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import com.pampoukidis.streamcoretv.web.navigation.isDiagnosticRoute
import com.pampoukidis.streamcoretv.web.navigation.requiresSelectedProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.Koin

/** Converts validated SDK context into routes; credentials and profile persistence belong to the SDK. */
internal class WebProductCoordinator(
    private val client: StreamCoreClient,
    private val navigation: WebNavigationController,
    initialSessionRestorationResult: StreamCoreResult<StreamCoreContext>? = null,
) {
    constructor(
        koin: Koin,
        navigation: WebNavigationController,
        initialSessionRestorationResult: StreamCoreResult<StreamCoreContext>? = null,
    ) : this(
        client = koin.get<StreamCoreClient>(),
        navigation = navigation,
        initialSessionRestorationResult = initialSessionRestorationResult,
    )

    private var pendingSessionRestorationResult = initialSessionRestorationResult
    var autoEnterSingleProfile: Boolean = true
        private set

    fun profileEntryStarted() {
        autoEnterSingleProfile = false
    }

    val context: StateFlow<StreamCoreContext>
        get() = client.context

    val selectedProfile: StreamCoreProfile?
        get() = context.value.profile

    suspend fun initialize(): WebProductInitialization {
        val result = try {
            val initialResult = pendingSessionRestorationResult
            pendingSessionRestorationResult = null
            initialResult ?: client.auth.restoreSession()
        } catch (exception: CancellationException) {
            navigation.replace(WebRoute.Login)
            throw exception
        } catch (_: Throwable) {
            navigation.replace(WebRoute.Login)
            return WebProductInitialization.ReadyWithError(sessionRestorationError())
        }
        synchronizeContext()
        return when (result) {
            is StreamCoreResult.Success -> WebProductInitialization.Ready
            is StreamCoreResult.Failure -> WebProductInitialization.ReadyWithError(result.error)
        }
    }

    suspend fun loginSucceeded(): StreamCoreError? {
        autoEnterSingleProfile = true
        navigation.navigate(if (context.value.account == null) WebRoute.Login else WebRoute.Profiles)
        return null
    }

    suspend fun profileSelected(profile: StreamCoreProfile): StreamCoreError? {
        if (context.value.profile?.id != profile.id) {
            return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.ProfileMismatch)
        }
        navigation.navigate(WebRoute.Home)
        return null
    }

    suspend fun changeProfile(): StreamCoreError? {
        autoEnterSingleProfile = false
        return when (val result = client.profiles.clearSelection()) {
            is StreamCoreResult.Success -> {
                navigation.navigate(WebRoute.Profiles)
                null
            }
            is StreamCoreResult.Failure -> {
                synchronizeContext()
                result.error
            }
        }
    }

    suspend fun logout(): StreamCoreError? {
        val result = try {
            client.auth.logout()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            return StreamCoreError.Unknown(
                source = StreamCoreErrorSource(operation = "logout", backendCode = "AUTH_LOGOUT_FAILURE"),
            )
        }
        synchronizeContext()
        return when (result) {
            is StreamCoreResult.Success -> null
            is StreamCoreResult.Failure -> result.error
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun reconcileProfiles(profiles: List<StreamCoreProfile>): StreamCoreError? {
        // Loading/mutating profiles has already reconciled the SDK's active context.
        synchronizeContext()
        return null
    }

    suspend fun reconcileProfilesFromRepository(): StreamCoreError? {
        val result = client.profiles.getProfiles()
        synchronizeContext()
        return when (result) {
            is StreamCoreResult.Success -> null
            is StreamCoreResult.Failure -> result.error
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun handleError(error: StreamCoreError) {
        // The SDK classifies authoritative rejection; a form error never invalidates a session here.
        synchronizeContext()
    }

    fun synchronizeContext() {
        sanitizeRoute(navigation.route.value)
    }

    fun sanitizeRoute(route: WebRoute) {
        val canonical = canonicalRoute(route)
        if (canonical != route) navigation.replace(canonical)
    }

    fun canonicalRoute(route: WebRoute): WebRoute {
        if (route.isDiagnosticRoute()) return route
        if (context.value.account == null || context.value.isClosed) return WebRoute.Login
        if (route is WebRoute.Root || route is WebRoute.Login || route is WebRoute.AuthenticatedLanding) {
            return if (selectedProfile == null) WebRoute.Profiles else WebRoute.Home
        }
        if (route.requiresSelectedProfile() && selectedProfile == null) return WebRoute.Profiles
        return route
    }
}

private fun sessionRestorationError(): StreamCoreError {
    return StreamCoreError.Unknown(
        source = StreamCoreErrorSource(operation = "bootstrapAuth", backendCode = "AUTH_BOOTSTRAP_FAILURE"),
    )
}

internal sealed interface WebProductInitialization {
    data object Ready : WebProductInitialization
    data class ReadyWithError(val error: StreamCoreError) : WebProductInitialization
}
