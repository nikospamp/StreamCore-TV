package com.pampoukidis.streamcoretv.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun `top level destinations create profile scoped routes`() {
        val profileId = "profile-1"

        assertEquals(
            AppRoute.Home(profileId = profileId),
            TopLevelDestination.Home.route(profileId),
        )
        assertEquals(
            AppRoute.Search(profileId = profileId),
            TopLevelDestination.Search.route(profileId),
        )
        assertEquals(
            AppRoute.Library(profileId = profileId),
            TopLevelDestination.Library.route(profileId),
        )
    }

    @Test
    fun `top level order keeps home as the back stack anchor`() {
        assertEquals(
            listOf(
                TopLevelDestination.Home,
                TopLevelDestination.Search,
                TopLevelDestination.Library,
            ),
            TopLevelDestination.entries,
        )
    }
}
