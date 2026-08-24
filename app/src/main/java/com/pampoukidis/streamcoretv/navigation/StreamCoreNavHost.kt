package com.pampoukidis.streamcoretv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.general.Platform
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreMotionDurations
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform
import com.pampoukidis.streamcoretv.feature.details.mobile.details.MobileDetailsRoute
import com.pampoukidis.streamcoretv.feature.details.tablet.details.TabletDetailsRoute
import com.pampoukidis.streamcoretv.feature.details.tv.details.TvDetailsRoute
import com.pampoukidis.streamcoretv.feature.home.mobile.home.MobileHomeRoute
import com.pampoukidis.streamcoretv.feature.home.tablet.home.TabletHomeRoute
import com.pampoukidis.streamcoretv.feature.home.tv.home.TvHomeRoute
import com.pampoukidis.streamcoretv.feature.library.mobile.library.MobileLibraryRoute
import com.pampoukidis.streamcoretv.feature.login.mobile.login.MobileLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tablet.login.TabletLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tv.login.TvLoginRoute
import com.pampoukidis.streamcoretv.feature.player.mobile.player.MobilePlayerRoute
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.mobile.editor.MobileProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles.MobileProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tablet.editor.TabletProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.tablet.profiles.TabletProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tv.editor.TvProfileEditorRoute
import com.pampoukidis.streamcoretv.feature.profiles.tv.profiles.TvProfilesRoute
import com.pampoukidis.streamcoretv.feature.search.mobile.search.MobileSearchRoute
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import kotlin.reflect.typeOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun StreamCoreNavHost(
    startDestination: AppRoute,
    onActiveProfileChanged: (String?) -> Unit,
    onError: (AppError) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val platform = rememberLoginPlatform()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevelDestination = currentBackStackEntry
        ?.destination
        ?.mobileTopLevelDestination()
    val currentTopLevelProfileId = currentBackStackEntry?.topLevelProfileId()
    val mobileBottomContentPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() + MobileBottomContentClearance
    var selectedContent by remember { mutableStateOf<ContentModel?>(null) }
    var selectedContentKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProfile by remember { mutableStateOf<ProfileModel?>(null) }
    var displayedTopLevelDestination by remember {
        mutableStateOf(MobileTopLevelDestination.Home)
    }
    var displayedTopLevelProfileId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTopLevelDestination, currentTopLevelProfileId) {
        if (currentTopLevelDestination != null && currentTopLevelProfileId != null) {
            displayedTopLevelDestination = currentTopLevelDestination
            displayedTopLevelProfileId = currentTopLevelProfileId
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            val sharedTransitionScope = this

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    if (isMobileTopLevelSwitch()) {
                        fadeIn(animationSpec = tween(MobileTabTransitionMillis))
                    } else {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = StreamCoreMotionDurations.NavigationEnterMillis,
                                delayMillis = StreamCoreMotionDurations.NavigationEnterDelayMillis,
                            ),
                        ) + slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(
                                durationMillis = StreamCoreMotionDurations.NavigationSlideMillis,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            if (isMobileTopLevelSwitch()) {
                                MobileTabTransitionMillis
                            } else {
                                StreamCoreMotionDurations.NavigationExitMillis
                            },
                        ),
                    )
                },
                popEnterTransition = {
                    if (isMobileTopLevelSwitch()) {
                        fadeIn(animationSpec = tween(MobileTabTransitionMillis))
                    } else {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = StreamCoreMotionDurations.NavigationEnterMillis,
                                delayMillis = StreamCoreMotionDurations.NavigationEnterDelayMillis,
                            ),
                        ) + slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(
                                durationMillis = StreamCoreMotionDurations.NavigationSlideMillis,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            if (isMobileTopLevelSwitch()) {
                                MobileTabTransitionMillis
                            } else {
                                StreamCoreMotionDurations.NavigationExitMillis
                            },
                        ),
                    )
                },
        ) {
            composable<AppRoute.Login> {
                LoginDestination(
                    onLoginSucceeded = {
                        selectedContent = null
                        selectedContentKey = null
                        onActiveProfileChanged(null)
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
                    sharedElementScope = StreamCoreSharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    ),
                    onProfileSelected = { profile ->
                        selectedContent = null
                        selectedContentKey = null
                        selectedProfile = profile
                        onActiveProfileChanged(profile.id)
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
                        selectedContentKey = null
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
                        selectedContentKey = null
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
                    activeProfile = selectedProfile?.takeIf { profile ->
                        profile.id == route.profileId
                    },
                    selectedContentKey = selectedContentKey,
                    mobileBottomContentPadding = mobileBottomContentPadding,
                    sharedElementScope = StreamCoreSharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    ),
                    onContentSelected = { content ->
                        selectedContent = content
                        selectedContentKey = content.sharedContentKey()
                        navController.navigate(
                            AppRoute.AssetDetails(
                                profileId = route.profileId,
                                contentId = content.id,
                                sourceRow = content.row,
                            ),
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onProfileSelected = {
                        selectedContent = null
                        selectedContentKey = null
                        navController.clearMobileTopLevelState(profileId = route.profileId)
                        onActiveProfileChanged(null)
                        navController.navigate(AppRoute.Profiles) {
                            popUpTo<AppRoute.Home> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onError = onError,
                )
            }

            composable<AppRoute.Search> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.Search>()

                MobileSearchRoute(
                    profileId = route.profileId,
                    selectedContentKey = selectedContentKey,
                    bottomContentPadding = mobileBottomContentPadding,
                    onContentSelected = { content ->
                        selectedContent = content
                        selectedContentKey = content.sharedContentKey()
                        navController.navigate(
                            AppRoute.AssetDetails(
                                profileId = route.profileId,
                                contentId = content.id,
                                sourceRow = content.row,
                            ),
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        selectedContent = null
                        selectedContentKey = null
                        navController.popBackStack()
                    },
                    sharedElementScope = StreamCoreSharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    ),
                )
            }

                composable<AppRoute.Library> { backStackEntry ->
                    val route = backStackEntry.toRoute<AppRoute.Library>()

                    MobileLibraryRoute(
                        profileId = route.profileId,
                        activeProfile = selectedProfile?.takeIf { profile ->
                            profile.id == route.profileId
                        },
                        selectedContentKey = selectedContentKey,
                        onContentSelected = { content ->
                            selectedContent = content
                            selectedContentKey = content.sharedContentKey()
                            navController.navigate(
                                AppRoute.AssetDetails(
                                    profileId = route.profileId,
                                    contentId = content.id,
                                    sourceRow = content.row,
                                ),
                            ) {
                                launchSingleTop = true
                            }
                        },
                        onProfileSelected = {
                            selectedContent = null
                            selectedContentKey = null
                            navController.clearMobileTopLevelState(profileId = route.profileId)
                            onActiveProfileChanged(null)
                            navController.navigate(AppRoute.Profiles) {
                                popUpTo<AppRoute.Home> {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onError = onError,
                        sharedElementScope = StreamCoreSharedElementScope(
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this,
                        ),
                    )
                }

            composable<AppRoute.AssetDetails>(
                typeMap = mapOf(typeOf<ContentModel?>() to ContentModelNavType)
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.AssetDetails>()

                val initialContent = (
                        route.initialContent ?: selectedContent?.takeIf { content ->
                            content.id == route.contentId
                        }
                        )?.withSourceRow(route.sourceRow)

                DetailsDestination(
                    profileId = route.profileId,
                    contentId = route.contentId,
                    initialContent = initialContent,
                    sharedElementScope = StreamCoreSharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    ),
                    onRecommendationSelected = { content ->
                        selectedContent = content
                        selectedContentKey = content.sharedContentKey()
                        navController.navigate(
                            AppRoute.AssetDetails(
                                profileId = route.profileId,
                                contentId = content.id,
                                sourceRow = content.row,
                                initialContent = content
                            ),
                        ) {
                            popUpTo(backStackEntry.destination.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onPlaySelected = { request ->
                        navController.navigate(
                            AppRoute.Player(
                                profileId = request.profileId,
                                contentId = request.contentId,
                                contentSnapshot = request.contentSnapshot,
                            ),
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onError = onError,
                )
            }

            composable<AppRoute.Player>(
                typeMap = mapOf(typeOf<ContentModel>() to ContentModelNavType),
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.Player>()
                MobilePlayerRoute(
                    request = PlaybackRequestModel(
                        profileId = route.profileId,
                        contentId = route.contentId,
                        contentSnapshot = route.contentSnapshot,
                    ),
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }

        AnimatedVisibility(
            visible = platform == Platform.Mobile && currentTopLevelDestination != null,
            enter = fadeIn(animationSpec = tween(MobileBarVisibilityMillis)) +
                    slideInVertically(
                        animationSpec = tween(MobileBarVisibilityMillis),
                        initialOffsetY = { height -> height / 2 },
                    ),
            exit = fadeOut(animationSpec = tween(MobileBarVisibilityMillis)) +
                    slideOutVertically(
                        animationSpec = tween(MobileBarVisibilityMillis),
                        targetOffsetY = { height -> height / 2 },
                    ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val profileId = displayedTopLevelProfileId
            if (profileId != null) {
                MobileNavigationBar(
                    selectedDestination = displayedTopLevelDestination,
                    onDestinationSelected = { destination ->
                        if (destination != currentTopLevelDestination) {
                            selectedContent = null
                            selectedContentKey = null
                            navController.navigateToMobileTopLevel(
                                destination = destination,
                                profileId = profileId,
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding)
                        .windowInsetsPadding(
                            WindowInsets.navigationBars
                                .union(WindowInsets.ime)
                                .only(WindowInsetsSides.Bottom),
                        )
                        .padding(bottom = StreamCoreDimens.Spacing.Medium),
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isMobileTopLevelSwitch(): Boolean {
    return initialState.destination.mobileTopLevelDestination() != null &&
            targetState.destination.mobileTopLevelDestination() != null
}

private fun NavBackStackEntry.topLevelProfileId(): String? {
    return when (destination.mobileTopLevelDestination()) {
        MobileTopLevelDestination.Home -> toRoute<AppRoute.Home>().profileId
        MobileTopLevelDestination.Search -> toRoute<AppRoute.Search>().profileId
        MobileTopLevelDestination.Library -> toRoute<AppRoute.Library>().profileId
        null -> null
    }
}

private fun NavHostController.navigateToMobileTopLevel(
    destination: MobileTopLevelDestination,
    profileId: String,
) {
    navigate(destination.route(profileId)) {
        popUpTo<AppRoute.Home> {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.clearMobileTopLevelState(profileId: String) {
    clearBackStack(AppRoute.Search(profileId = profileId))
    clearBackStack(AppRoute.Library(profileId = profileId))
}

private fun ContentModel.sharedContentKey(): String {
    return StreamCoreSharedKey.content(
        contentId = id,
        row = row,
    )
}

private fun ContentModel.withSourceRow(sourceRow: String?): ContentModel {
    return if (sourceRow == null || row == sourceRow) {
        this
    } else {
        copy(row = sourceRow)
    }
}

private const val MobileTabTransitionMillis = 180
private const val MobileBarVisibilityMillis = 160
private val MobileBottomContentClearance = 108.dp

internal fun startDestinationForAuthState(
    authState: AuthStateModel,
    activeProfileId: String?,
): AppRoute {
    return when {
        authState is AuthStateModel.LoggedOut -> AppRoute.Login
        activeProfileId != null -> AppRoute.Home(profileId = activeProfileId)
        else -> AppRoute.Profiles
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
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileProfilesRoute(
            onProfileSelected = onProfileSelected,
            onCreateProfile = onCreateProfile,
            onEditProfile = onEditProfile,
            onError = onError,
            sharedElementScope = sharedElementScope,
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
    activeProfile: ProfileModel?,
    selectedContentKey: String?,
    mobileBottomContentPadding: Dp,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
    onProfileSelected: () -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileHomeRoute(
            profileId = profileId,
            activeProfile = activeProfile,
            selectedContentKey = selectedContentKey,
            onContentSelected = onContentSelected,
            onProfileSelected = onProfileSelected,
            onError = onError,
            bottomContentPadding = mobileBottomContentPadding,
            sharedElementScope = sharedElementScope,
        )

        Platform.Tablet -> TabletHomeRoute(
            profileId = profileId,
            selectedContentKey = selectedContentKey,
            onContentSelected = onContentSelected,
            onProfileSelected = onProfileSelected,
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
    onPlaySelected: (PlaybackRequestModel) -> Unit,
    onBack: () -> Unit,
    onError: (AppError) -> Unit,
) {
    when (rememberLoginPlatform()) {
        Platform.Mobile -> MobileDetailsRoute(
            profileId = profileId,
            contentId = contentId,
            onRecommendationSelected = onRecommendationSelected,
            onPlaySelected = onPlaySelected,
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
