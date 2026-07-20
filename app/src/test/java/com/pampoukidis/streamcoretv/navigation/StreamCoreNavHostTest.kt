package com.pampoukidis.streamcoretv.navigation

import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamCoreNavHostTest {

    @Test
    fun `logged out auth state starts at login`() {
        val result = startDestinationForAuthState(
            authState = AuthStateModel.LoggedOut,
            activeProfileId = null,
        )

        assertEquals(AppRoute.Login, result)
    }

    @Test
    fun `logged in auth state starts at profiles`() {
        val result = startDestinationForAuthState(
            authState = AuthStateModel.LoggedIn(account = null),
            activeProfileId = null,
        )

        assertEquals(AppRoute.Profiles, result)
    }

    @Test
    fun `logged in auth state with active profile starts at home`() {
        val result = startDestinationForAuthState(
            authState = AuthStateModel.LoggedIn(account = null),
            activeProfileId = "profile-1",
        )

        assertEquals(AppRoute.Home(profileId = "profile-1"), result)
    }

    @Test
    fun `logged out auth state ignores active profile`() {
        val result = startDestinationForAuthState(
            authState = AuthStateModel.LoggedOut,
            activeProfileId = "profile-1",
        )

        assertEquals(AppRoute.Login, result)
    }
}
