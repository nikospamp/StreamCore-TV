package com.pampoukidis.streamcoretv.navigation

import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamCoreNavHostTest {

    @Test
    fun `logged out auth state starts at login`() {
        val result = startDestinationForAuthState(AuthStateModel.LoggedOut)

        assertEquals(AppRoute.Login, result)
    }

    @Test
    fun `logged in auth state starts at profiles`() {
        val result = startDestinationForAuthState(AuthStateModel.LoggedIn(account = null))

        assertEquals(AppRoute.Profiles, result)
    }
}
