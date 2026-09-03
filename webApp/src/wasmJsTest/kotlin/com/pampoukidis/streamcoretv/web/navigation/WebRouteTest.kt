package com.pampoukidis.streamcoretv.web.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class WebRouteTest {
    @Test
    fun parsesDirectDetailsAndPlayerUrlsWithIdsOnly() {
        assertEquals(WebRoute.Details("603"), WebRoute.parse("/details/603"))
        assertEquals(WebRoute.Player("movie_603"), WebRoute.parse("/player/movie_603"))
    }

    @Test
    fun rejectsPayloadLikeAndUnknownRoutes() {
        assertEquals(WebRoute.Root, WebRoute.parse("/details/%7Bcontent%7D"))
        assertEquals(WebRoute.Root, WebRoute.parse("/unknown/603"))
    }

    @Test
    fun routePathsRemainBrowserSafe() {
        assertEquals("/details/movie-603", WebRoute.Details("movie-603").path)
        assertEquals("/player/movie_603", WebRoute.Player("movie_603").path)
        assertEquals(WebRoute.Login, WebRoute.parse("/login?source=history"))
        assertEquals(WebRoute.EditProfile("profile-1"), WebRoute.parse("/profiles/profile-1/edit"))
    }
}
