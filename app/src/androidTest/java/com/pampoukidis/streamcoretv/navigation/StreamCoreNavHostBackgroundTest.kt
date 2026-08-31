package com.pampoukidis.streamcoretv.navigation

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.pampoukidis.streamcoretv.MainActivity
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StreamCoreNavHostBackgroundTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun darkNavigationNeverExposesLightBackingDuringPushOrPop() {
        assertNavigationBackground(darkTheme = true)
    }

    @Test
    fun lightNavigationUsesTheCurrentThemeBackgroundDuringPushOrPop() {
        assertNavigationBackground(darkTheme = false)
    }

    @Test
    fun loggedOutAuthStateResetsBackStackToLogin() {
        lateinit var navController: NavHostController
        var authState by mutableStateOf<AuthStateModel>(AuthStateModel.LoggedIn(account = null))

        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent {
                MaterialTheme {
                    navController = rememberNavController()
                    StreamCoreNavHost(
                        startDestination = AppRoute.Profiles,
                        authState = authState,
                        isLogoutConfirmationVisible = false,
                        isLogoutInProgress = false,
                        onActiveProfileChanged = {},
                        onLogoutRequested = {},
                        onError = {},
                        navController = navController,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            navController.navigate(AppRoute.CreateProfile(fromLogin = false))
            authState = AuthStateModel.LoggedOut
        }

        composeRule.waitUntil {
            navController.currentDestination?.hasRoute<AppRoute.Login>() == true
        }
        composeRule.runOnIdle {
            assertFalse(navController.popBackStack())
        }
    }

    private fun assertNavigationBackground(darkTheme: Boolean) {
        val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
        lateinit var navController: NavHostController

        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent {
                MaterialTheme(colorScheme = colorScheme) {
                    navController = rememberNavController()
                    // Model the light native window behind the real navigation hierarchy.
                    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                        StreamCoreNavHost(
                            startDestination = AppRoute.Profiles,
                            authState = AuthStateModel.LoggedIn(account = null),
                            isLogoutConfirmationVisible = false,
                            isLogoutInProgress = false,
                            onActiveProfileChanged = {},
                            onLogoutRequested = {},
                            onError = {},
                            navController = navController,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnIdle {
            navController.navigate(AppRoute.CreateProfile(fromLogin = false))
        }
        assertTransitionFrames(background = colorScheme.background)

        composeRule.runOnIdle {
            assertTrue(navController.popBackStack())
        }
        assertTransitionFrames(background = colorScheme.background)
    }

    private fun assertTransitionFrames(background: Color) {
        // Sample intermediate frames, including when the outgoing fade has completed but
        // the incoming slide has not. Final-frame-only assertions miss the white flash.
        repeat(20) { frame ->
            composeRule.mainClock.advanceTimeByFrame()
            val pixels = composeRule.onRoot().captureToImage().toPixelMap()
            val actual = pixels[1, pixels.height - 2]
            val message = "Navigation background changed at frame $frame"
            assertEquals(message, background.red, actual.red, ColorTolerance)
            assertEquals(message, background.green, actual.green, ColorTolerance)
            assertEquals(message, background.blue, actual.blue, ColorTolerance)
        }
    }

    private companion object {
        const val ColorTolerance = 0.01f
    }
}
