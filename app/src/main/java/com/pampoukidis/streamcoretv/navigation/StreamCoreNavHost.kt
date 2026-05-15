package com.pampoukidis.streamcoretv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.LoginPlatform
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform
import com.pampoukidis.streamcoretv.feature.login.mobile.MobileLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tablet.TabletLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tv.TvLoginRoute
import com.pampoukidis.streamcoretv.feature.profiles.mobile.MobileProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tablet.TabletProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tv.TvProfilesRoute

private const val NavigationAnimationMillis = 250

@Composable
internal fun StreamCoreNavHost(
    onError: (AppError) -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Login,
        enterTransition = {
            fadeIn(
                animationSpec = tween(NavigationAnimationMillis),
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NavigationAnimationMillis),
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(NavigationAnimationMillis),
            ) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NavigationAnimationMillis),
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(NavigationAnimationMillis),
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NavigationAnimationMillis),
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(NavigationAnimationMillis),
            ) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NavigationAnimationMillis),
            )
        },
    ) {
        composable<AppRoute.Login> {
            LoginDestination(
                onLoginSucceeded = {
                    navController.navigate(AppRoute.Profiles) {
                        popUpTo<AppRoute.Login> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onForgotPassword = {},
                onCreateAccount = {},
                onHelp = {},
                onError = onError,
            )
        }

        composable<AppRoute.Profiles> {
            ProfilesDestination(
                onProfileSelected = { profile ->
                    navController.navigate(
                        AppRoute.Authenticated(profileId = profile.id),
                    ) {
                        popUpTo<AppRoute.Profiles> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onError = onError,
            )
        }

        composable<AppRoute.Authenticated> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Authenticated>()

            AuthenticatedPlaceholder(
                profileId = route.profileId,
            )
        }
    }
}

@Composable
private fun LoginDestination(
    onLoginSucceeded: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        LoginPlatform.Mobile -> MobileLoginRoute(
            onLoginSucceeded = onLoginSucceeded,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
            onError = onError,
        )

        LoginPlatform.Tablet -> TabletLoginRoute(
            onLoginSucceeded = onLoginSucceeded,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
            onError = onError,
        )

        LoginPlatform.Tv -> TvLoginRoute(
            onLoginSucceeded = onLoginSucceeded,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
            onError = onError,
        )
    }
}

@Composable
private fun ProfilesDestination(
    onProfileSelected: (ProfileModel) -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        LoginPlatform.Mobile -> MobileProfilesRoute(
            onProfileSelected = onProfileSelected,
            onError = onError,
        )

        LoginPlatform.Tablet -> TabletProfilesRoute(
            onProfileSelected = onProfileSelected,
            onError = onError,
        )

        LoginPlatform.Tv -> TvProfilesRoute(
            onProfileSelected = onProfileSelected,
            onError = onError,
        )
    }
}

@Composable
private fun AuthenticatedPlaceholder(
    profileId: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "Active profile id: $profileId",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@PreviewMobile
@Composable
private fun AuthenticatedPlaceholderPreview() {
    StreamCoreTVTheme {
        AuthenticatedPlaceholder(
            profileId = "adult-profile",
        )
    }
}