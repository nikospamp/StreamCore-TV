package com.pampoukidis.streamcoretv.web.product

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.clearTmdbAuthSessionPreferences
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.di.TMDB_AUTH_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import com.pampoukidis.streamcoretv.web.navigation.isDiagnosticRoute
import com.pampoukidis.streamcoretv.web.navigation.requiresSelectedProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.Koin
import org.koin.core.qualifier.named

internal class WebProductCoordinator(
    private val authenticateRepository: AuthenticateRepository,
    private val profileRepository: ProfileRepository,
    private val authStore: DataStore<Preferences>,
    accountId: String,
    private val navigation: WebNavigationController,
) {
    private val selectedProfileIdKey = selectedProfileIdKey(accountId)

    constructor(
        koin: Koin,
        navigation: WebNavigationController,
    ) : this(
        authenticateRepository = koin.get<AuthenticateRepository>(),
        profileRepository = koin.get<ProfileRepository>(),
        authStore = koin.get<DataStore<Preferences>>(named(TMDB_AUTH_STORE_QUALIFIER)),
        accountId = koin.get<TmdbRuntimeConfig>().accountId,
        navigation = navigation,
    )

    var selectedProfile: ProfileModel? = null
        private set
    private var authenticated: Boolean = false

    suspend fun initialize(): WebProductInitialization {
        val bootstrapResult = try {
            authenticateRepository.bootstrapAuth()
        } catch (throwable: CancellationException) {
            authenticated = false
            selectedProfile = null
            navigation.replace(WebRoute.Login)
            throw throwable
        } catch (_: Throwable) {
            authenticated = false
            selectedProfile = null
            navigation.replace(WebRoute.Login)
            return WebProductInitialization.ReadyWithError(authBootstrapError())
        }
        return when (bootstrapResult) {
            is AppResult.Success -> when (bootstrapResult.value) {
                AuthStateModel.LoggedOut -> {
                    authenticated = false
                    selectedProfile = null
                    try {
                        clearSelectedProfile()
                        WebProductInitialization.Ready
                    } catch (throwable: CancellationException) {
                        throw throwable
                    } catch (_: Throwable) {
                        WebProductInitialization.ReadyWithError(profileSelectionStorageError())
                    } finally {
                        navigation.replace(WebRoute.Login)
                    }
                }
                is AuthStateModel.LoggedIn -> {
                    authenticated = true
                    restoreAuthenticatedRoute()
                }
            }
            is AppResult.Failure -> {
                authenticated = false
                selectedProfile = null
                try {
                    if (bootstrapResult.error.invalidatesPersistedSession()) {
                        try {
                            invalidateSessionPersistence()
                        } catch (throwable: CancellationException) {
                            throw throwable
                        } catch (_: Throwable) {
                            // Preserve the primary bootstrap failure while remaining fail-closed.
                        }
                    }
                    WebProductInitialization.ReadyWithError(bootstrapResult.error)
                } finally {
                    navigation.replace(WebRoute.Login)
                }
            }
        }
    }

    suspend fun loginSucceeded() {
        authenticated = true
        clearSelectedProfile()
        navigation.navigate(WebRoute.Profiles)
    }

    suspend fun profileSelected(profile: ProfileModel) {
        selectedProfile = profile
        authStore.edit { preferences ->
            preferences[selectedProfileIdKey] = profile.id
        }
        navigation.navigate(WebRoute.Home)
    }

    suspend fun changeProfile() {
        clearSelectedProfile()
        navigation.navigate(WebRoute.Profiles)
    }

    suspend fun logout(): AppError? {
        return when (val result = authenticateRepository.logoutUser()) {
            is AppResult.Success -> {
                authenticated = false
                selectedProfile = null
                val cleanupError = try {
                    clearSelectedProfile()
                    null
                } catch (throwable: CancellationException) {
                    navigation.replace(WebRoute.Login)
                    throw throwable
                } catch (_: Throwable) {
                    profileSelectionStorageError()
                }
                navigation.replace(WebRoute.Login)
                cleanupError
            }
            is AppResult.Failure -> result.error
        }
    }

    suspend fun reconcileProfiles(profiles: List<ProfileModel>) {
        val activeProfile = selectedProfile ?: return
        val currentProfile = profiles.firstOrNull { profile -> profile.id == activeProfile.id }
        if (currentProfile == null) {
            clearSelectedProfile()
        } else if (currentProfile != activeProfile) {
            selectedProfile = currentProfile
        }
    }

    suspend fun reconcileProfilesFromRepository(): AppError? {
        return when (val result = profileRepository.getProfiles()) {
            is AppResult.Success -> {
                reconcileProfiles(result.value)
                null
            }
            is AppResult.Failure -> result.error
        }
    }

    suspend fun handleError(error: AppError) {
        if (error is AppError.SessionExpired || error is AppError.Authentication || error is AppError.Unauthorized) {
            authenticated = false
            selectedProfile = null
            try {
                try {
                    invalidateSessionPersistence()
                } catch (throwable: CancellationException) {
                    throw throwable
                } catch (_: Throwable) {
                    // The originating auth error remains primary; routing still fails closed.
                }
            } finally {
                navigation.replace(WebRoute.Login)
            }
        }
    }

    fun sanitizeRoute(route: WebRoute) {
        val canonicalRoute = canonicalRoute(route)
        if (canonicalRoute != route) {
            navigation.replace(canonicalRoute)
        }
    }

    fun canonicalRoute(route: WebRoute): WebRoute {
        if (route.isDiagnosticRoute()) {
            return route
        }
        if (!authenticated && route !is WebRoute.Login) {
            return WebRoute.Login
        }
        if (!authenticated) {
            return route
        }
        if (route is WebRoute.Root || route is WebRoute.Login || route is WebRoute.AuthenticatedLanding) {
            return if (selectedProfile == null) WebRoute.Profiles else WebRoute.Home
        }
        if (route.requiresSelectedProfile() && selectedProfile == null) {
            return WebRoute.Profiles
        }
        return route
    }

    private suspend fun restoreAuthenticatedRoute(): WebProductInitialization {
        val selectedProfileId = try {
            authStore.data.first()[selectedProfileIdKey]
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (_: Throwable) {
            selectedProfile = null
            navigation.replace(safeProfileRestoreRoute())
            return WebProductInitialization.ReadyWithError(profileSelectionStorageError())
        }
        val profiles = when (val result = profileRepository.getProfiles()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> {
                selectedProfile = null
                navigation.replace(safeProfileRestoreRoute())
                return WebProductInitialization.ReadyWithError(result.error)
            }
        }
        selectedProfile = profiles.firstOrNull { profile -> profile.id == selectedProfileId }
        if (selectedProfileId != null && selectedProfile == null) {
            clearSelectedProfile()
        }
        val requestedRoute = navigation.route.value
        val destination = when {
            requestedRoute.isDiagnosticRoute() -> requestedRoute
            requestedRoute is WebRoute.Profiles || requestedRoute is WebRoute.CreateProfile ||
                requestedRoute is WebRoute.EditProfile -> requestedRoute
            selectedProfile == null -> WebRoute.Profiles
            requestedRoute.requiresSelectedProfile() && requestedRoute !is WebRoute.AuthenticatedLanding -> {
                requestedRoute
            }
            else -> WebRoute.Home
        }
        navigation.replace(destination)
        return WebProductInitialization.Ready
    }

    private suspend fun clearSelectedProfile() {
        selectedProfile = null
        authStore.edit { preferences ->
            if (preferences[selectedProfileIdKey] != null) {
                preferences.remove(selectedProfileIdKey)
            }
        }
    }

    private suspend fun invalidateSessionPersistence() {
        authStore.edit { preferences ->
            preferences.clearTmdbAuthSessionPreferences()
            if (preferences[selectedProfileIdKey] != null) {
                preferences.remove(selectedProfileIdKey)
            }
        }
    }

    private fun safeProfileRestoreRoute(): WebRoute {
        return if (navigation.route.value.isDiagnosticRoute()) {
            navigation.route.value
        } else {
            WebRoute.Profiles
        }
    }
}

internal fun selectedProfileIdKey(accountId: String): Preferences.Key<String> {
    require(accountId.isNotBlank())
    val encodedAccountScope = accountId.encodeToByteArray().joinToString(separator = "") { byte ->
        (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
    }
    return stringPreferencesKey("web_selected_profile_id.$encodedAccountScope")
}

private fun profileSelectionStorageError(): AppError {
    return AppError.Unknown(
        source = ErrorSource(
            operation = "restoreSelectedProfile",
            backendCode = "PROFILE_SELECTION_STORAGE_FAILURE",
        ),
    )
}

private fun authBootstrapError(): AppError {
    return AppError.Unknown(
        source = ErrorSource(
            operation = "bootstrapAuth",
            backendCode = "AUTH_BOOTSTRAP_FAILURE",
        ),
    )
}

private fun AppError.invalidatesPersistedSession(): Boolean {
    return this is AppError.Authentication ||
        this is AppError.Unauthorized ||
        this is AppError.SessionExpired
}

internal sealed interface WebProductInitialization {
    data object Ready : WebProductInitialization
    data class ReadyWithError(val error: AppError) : WebProductInitialization
}
