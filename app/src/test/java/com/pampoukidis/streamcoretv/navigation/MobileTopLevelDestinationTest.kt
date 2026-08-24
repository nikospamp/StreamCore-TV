package com.pampoukidis.streamcoretv.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileTopLevelDestinationTest {

    @Test
    fun `top level destinations create profile scoped routes`() {
        val profileId = "profile-1"

        assertEquals(
            AppRoute.Home(profileId = profileId),
            MobileTopLevelDestination.Home.route(profileId),
        )
        assertEquals(
            AppRoute.Search(profileId = profileId),
            MobileTopLevelDestination.Search.route(profileId),
        )
        assertEquals(
            AppRoute.Library(profileId = profileId),
            MobileTopLevelDestination.Library.route(profileId),
        )
    }

    @Test
    fun `top level order keeps home as the back stack anchor`() {
        assertEquals(
            listOf(
                MobileTopLevelDestination.Home,
                MobileTopLevelDestination.Search,
                MobileTopLevelDestination.Library,
            ),
            MobileTopLevelDestination.entries,
        )
    }
}
