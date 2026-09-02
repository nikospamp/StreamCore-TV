package com.pampoukidis.streamcoretv.web.product

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.client.tmdb.data.di.TMDB_AUTH_STORE_QUALIFIER
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import kotlinx.coroutines.flow.first
import org.koin.core.Koin
import org.koin.core.qualifier.named

internal class WebProductCoordinator(
    private val koin: Koin,
    private val navigation: WebNavigationController,
) {
    private val authenticateRepository = koin.get<AuthenticateRepository>()
    private val profileRepository = koin.get<ProfileRepository>()
    private val authStore = koin.get<DataStore<Preferences>>(named(TMDB_AUTH_STORE_QUALIFIER))

    var selectedProfile: ProfileModel? = null
        private set
    private var authenticated: Boolean = false

    suspend fun initialize(): WebProductInitialization {
        return when (val result = authenticateRepository.bootstrapAuth()) {
            is AppResult.Success -> when (result.value) {
                AuthStateModel.LoggedOut -> {
                    authenticated = false
                    selectedProfile = null
                    navigation.replace(WebRoute.Login)
                    WebProductInitialization.Ready
                }
                is AuthStateModel.LoggedIn -> {
                    authenticated = true
                    restoreAuthenticatedRoute()
                }
            }
            is AppResult.Failure -> {
                authenticated = false
                selectedProfile = null
                navigation.replace(WebRoute.Login)
                WebProductInitialization.ReadyWithError(result.error)
            }
        }
    }

    suspend fun loginSucceeded() {
        authenticated = true
        selectedProfile = null
        navigation.navigate(WebRoute.Profiles)
    }

    suspend fun profileSelected(profile: ProfileModel) {
        selectedProfile = profile
        authStore.edit { preferences ->
            preferences[SelectedProfileIdKey] = profile.id
        }
        navigation.navigate(WebRoute.AuthenticatedLanding)
    }

    suspend fun changeProfile() {
        clearSelectedProfile()
        navigation.navigate(WebRoute.Profiles)
    }

    suspend fun handleError(error: AppError) {
        if (error is AppError.SessionExpired || error is AppError.Authentication || error is AppError.Unauthorized) {
            authenticated = false
            clearSelectedProfile()
            navigation.replace(WebRoute.Login)
        }
    }

    fun sanitizeRoute(route: WebRoute) {
        if (!authenticated && route !is WebRoute.Login && route !is WebRoute.Diagnostic) {
            navigation.replace(WebRoute.Login)
            return
        }
        if (authenticated && route is WebRoute.Login) {
            navigation.replace(if (selectedProfile == null) WebRoute.Profiles else WebRoute.AuthenticatedLanding)
            return
        }
        if (authenticated && selectedProfile == null && route is WebRoute.AuthenticatedLanding) {
            navigation.replace(WebRoute.Profiles)
        }
    }

    private suspend fun restoreAuthenticatedRoute(): WebProductInitialization {
        val selectedProfileId = authStore.data.first()[SelectedProfileIdKey]
        selectedProfile = when (val result = profileRepository.getProfiles()) {
            is AppResult.Success -> result.value.firstOrNull { it.id == selectedProfileId }
            is AppResult.Failure -> null
        }
        if (selectedProfileId != null && selectedProfile == null) {
            clearSelectedProfile()
        }
        val requestedRoute = navigation.route.value
        val destination = when {
            requestedRoute is WebRoute.Diagnostic -> WebRoute.Diagnostic
            requestedRoute is WebRoute.Profiles || requestedRoute is WebRoute.CreateProfile ||
                requestedRoute is WebRoute.EditProfile -> requestedRoute
            selectedProfile != null -> WebRoute.AuthenticatedLanding
            else -> WebRoute.Profiles
        }
        navigation.replace(destination)
        return WebProductInitialization.Ready
    }

    private suspend fun clearSelectedProfile() {
        selectedProfile = null
        authStore.edit { preferences ->
            preferences.remove(SelectedProfileIdKey)
        }
    }

    private companion object {
        val SelectedProfileIdKey = stringPreferencesKey("web_selected_profile_id")
    }
}

internal sealed interface WebProductInitialization {
    data object Ready : WebProductInitialization
    data class ReadyWithError(val error: AppError) : WebProductInitialization
}
