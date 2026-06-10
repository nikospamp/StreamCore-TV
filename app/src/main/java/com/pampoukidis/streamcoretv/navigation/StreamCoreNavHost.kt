package com.pampoukidis.streamcoretv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.general.Platform
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform
import com.pampoukidis.streamcoretv.feature.home.mobile.home.MobileHomeRoute
import com.pampoukidis.streamcoretv.feature.home.tablet.home.TabletHomeRoute
import com.pampoukidis.streamcoretv.feature.home.tv.home.TvHomeRoute
import com.pampoukidis.streamcoretv.feature.login.mobile.login.MobileLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tablet.login.TabletLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tv.login.TvLoginRoute
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.mobile.editor.MobileProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles.MobileProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tablet.editor.TabletProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.tablet.profiles.TabletProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tv.editor.TvProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.tv.profiles.TvProfilesRoute

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
                onCreateAccount = {
                    navController.navigate(AppRoute.CreateProfile(fromLogin = true))
                },
                onHelp = {},
                onError = onError,
            )
        }

        composable<AppRoute.Profiles> {
            ProfilesDestination(
                onProfileSelected = { profile ->
                    navController.navigate(
                        AppRoute.Home(profileId = profile.id),
                    ) {
                        popUpTo<AppRoute.Profiles> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onCreateProfile = {
                    navController.navigate(AppRoute.CreateProfile(fromLogin = false))
                },
                onEditProfile = { profileId ->
                    navController.navigate(AppRoute.EditProfile(profileId = profileId))
                },
                onError = onError,
            )
        }

        composable<AppRoute.CreateProfile> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.CreateProfile>()

            ProfileEditorDestination(
                mode = ProfileEditorMode.Create,
                profileId = null,
                onProfileSaved = {
                    navController.navigate(AppRoute.Profiles) {
                        if (route.fromLogin) {
                            popUpTo<AppRoute.Login> {
                                inclusive = true
                            }
                        } else {
                            popUpTo<AppRoute.Profiles> {
                                inclusive = true
                            }
                        }
                        launchSingleTop = true
                    }
                },
                onClose = {
                    navController.popBackStack()
                },
                onError = onError,
            )
        }

        composable<AppRoute.EditProfile> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.EditProfile>()

            ProfileEditorDestination(
                mode = ProfileEditorMode.Edit,
                profileId = route.profileId,
                onProfileSaved = {
                    navController.navigate(AppRoute.Profiles) {
                        popUpTo<AppRoute.Profiles> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onClose = {
                    navController.popBackStack()
                },
                onError = onError,
            )
        }

        composable<AppRoute.Home> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Home>()

            HomeDestination(
                profileId = route.profileId,
                onError = onError,
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
        Platform.Mobile -> MobileLoginRoute(
            onLoginSucceeded = onLoginSucceeded,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
            onError = onError,
        )

        Platform.Tablet -> TabletLoginRoute(
            onLoginSucceeded = onLoginSucceeded,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
            onError = onError,
        )

        Platform.Tv -> TvLoginRoute(
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
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileProfilesRoute(
            onProfileSelected = onProfileSelected,
            onCreateProfile = onCreateProfile,
            onEditProfile = onEditProfile,
            onError = onError,
        )

        Platform.Tablet -> TabletProfilesRoute(
            onProfileSelected = onProfileSelected,
            onCreateProfile = onCreateProfile,
            onEditProfile = onEditProfile,
            onError = onError,
        )

        Platform.Tv -> TvProfilesRoute(
            onProfileSelected = onProfileSelected,
            onCreateProfile = onCreateProfile,
            onEditProfile = onEditProfile,
            onError = onError,
        )
    }
}

@Composable
private fun ProfileEditorDestination(
    mode: ProfileEditorMode,
    profileId: String?,
    onProfileSaved: () -> Unit,
    onClose: () -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileProfileEditorRoute(
            mode = mode,
            profileId = profileId,
            onProfileSaved = onProfileSaved,
            onClose = onClose,
            onError = onError,
        )

        Platform.Tablet -> TabletProfileEditorRoute(
            mode = mode,
            profileId = profileId,
            onProfileSaved = onProfileSaved,
            onClose = onClose,
            onError = onError,
        )

        Platform.Tv -> TvProfileEditorRoute(
            mode = mode,
            profileId = profileId,
            onProfileSaved = onProfileSaved,
            onClose = onClose,
            onError = onError,
        )
    }
}

@Composable
private fun HomeDestination(
    profileId: String,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileHomeRoute(
            profileId = profileId,
            onError = onError,
        )

        Platform.Tablet -> TabletHomeRoute(
            profileId = profileId,
            onError = onError,
        )

        Platform.Tv -> TvHomeRoute(
            profileId = profileId,
            onError = onError,
        )
    }
}
