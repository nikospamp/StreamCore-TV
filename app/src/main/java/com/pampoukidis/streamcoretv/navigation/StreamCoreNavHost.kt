package com.pampoukidis.streamcoretv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.general.Platform
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreContentSharedIdentity
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform
import com.pampoukidis.streamcoretv.feature.details.mobile.details.MobileDetailsRoute
import com.pampoukidis.streamcoretv.feature.details.tablet.details.TabletDetailsRoute
import com.pampoukidis.streamcoretv.feature.details.tv.details.TvDetailsRoute
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
import kotlin.reflect.typeOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun StreamCoreNavHost(
    onError: (AppError) -> Unit,
) {
    val navController = rememberNavController()
    var selectedContent by remember { mutableStateOf<ContentModel?>(null) }

    SharedTransitionLayout {
        val sharedTransitionScope = this

        NavHost(
            navController = navController,
            startDestination = AppRoute.Login,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = NavigationFadeMillis,
                        delayMillis = NavigationFadeDelayMillis,
                    ),
                ) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(
                        durationMillis = NavigationSlideMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(NavigationExitFadeMillis),
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = NavigationFadeMillis,
                        delayMillis = NavigationFadeDelayMillis,
                    ),
                ) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(
                        durationMillis = NavigationSlideMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            },
            popExitTransition = {
                fadeOut(
                    animationSpec = tween(NavigationExitFadeMillis),
                )
            },
        ) {
            composable<AppRoute.Login> {
                LoginDestination(
                    onLoginSucceeded = {
                        selectedContent = null
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
                        selectedContent = null
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
                        selectedContent = null
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
                        selectedContent = null
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
                    selectedContentKey = selectedContent?.let { content ->
                        streamCoreContentSharedIdentity(
                            contentId = content.id,
                            row = content.row,
                        )
                    },
                    sharedElementScope = StreamCoreSharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    ),
                    onContentSelected = { content ->
                        selectedContent = content
                        navController.navigate(
                            AppRoute.AssetDetails(
                                profileId = route.profileId,
                                contentId = content.id,
                            ),
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onError = onError,
                )
            }

            composable<AppRoute.AssetDetails>(
                typeMap = mapOf(typeOf<ContentModel?>() to ContentModelNavType)
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.AssetDetails>()

                val initialContent = route.initialContent ?: selectedContent?.takeIf { content ->
                    content.id == route.contentId
                }

                DetailsDestination(
                    profileId = route.profileId,
                    contentId = route.contentId,
                    initialContent = initialContent,
                    sharedElementScope = StreamCoreSharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    ),
                    onRecommendationSelected = { content ->
                        navController.navigate(
                            AppRoute.AssetDetails(
                                profileId = route.profileId,
                                contentId = content.id,
                                initialContent = content
                            ),
                        ) {
                            popUpTo<AppRoute.Home> {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onError = onError,
                )
            }
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
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileHomeRoute(
            profileId = profileId,
            selectedContentKey = selectedContentKey,
            onContentSelected = onContentSelected,
            onError = onError,
            sharedElementScope = sharedElementScope,
        )

        Platform.Tablet -> TabletHomeRoute(
            profileId = profileId,
            selectedContentKey = selectedContentKey,
            onContentSelected = onContentSelected,
            onError = onError,
            sharedElementScope = sharedElementScope,
        )

        Platform.Tv -> TvHomeRoute(
            profileId = profileId,
            selectedContentKey = selectedContentKey,
            onContentSelected = onContentSelected,
            onError = onError,
            sharedElementScope = sharedElementScope,
        )
    }
}

@Composable
private fun DetailsDestination(
    profileId: String,
    contentId: String,
    initialContent: ContentModel?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onRecommendationSelected: (ContentModel) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileDetailsRoute(
            profileId = profileId,
            contentId = contentId,
            onRecommendationSelected = onRecommendationSelected,
            onBack = onBack,
            onError = onError,
            initialContent = initialContent,
            sharedElementScope = sharedElementScope,
        )

        Platform.Tablet -> TabletDetailsRoute(
            profileId = profileId,
            contentId = contentId,
            onRecommendationSelected = onRecommendationSelected,
            onBack = onBack,
            onError = onError,
            initialContent = initialContent,
            sharedElementScope = sharedElementScope,
        )

        Platform.Tv -> TvDetailsRoute(
            profileId = profileId,
            contentId = contentId,
            onRecommendationSelected = onRecommendationSelected,
            onBack = onBack,
            onError = onError,
            initialContent = initialContent,
            sharedElementScope = sharedElementScope,
        )
    }
}

private const val NavigationSlideMillis = 240
private const val NavigationFadeMillis = 140
private const val NavigationExitFadeMillis = 90
private const val NavigationFadeDelayMillis = 20