package com.pampoukidis.streamcoretv

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.auth.AppAuthEffect
import com.pampoukidis.streamcoretv.auth.AppAuthAction
import com.pampoukidis.streamcoretv.auth.AppAuthUiState
import com.pampoukidis.streamcoretv.auth.AppAuthViewModel
import com.pampoukidis.streamcoretv.auth.AppLogoutConfirmationDialog
import com.pampoukidis.streamcoretv.core.ui.avatar.LocalProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.components.ErrorHost
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.error.ErrorUiModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.navigation.StreamCoreNavHost
import com.pampoukidis.streamcoretv.navigation.startDestinationForAuthState
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val errorPresentationMapper: ErrorPresentationMapper by inject()

    private val profileAvatarArtworkResolver: ProfileAvatarArtworkResolver by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTvSoftInputMode()
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalProfileAvatarArtworkResolver provides profileAvatarArtworkResolver,
            ) {
                StreamCoreTheme {
                    val appAuthViewModel: AppAuthViewModel = koinViewModel()
                    val appAuthUiState by appAuthViewModel.uiState.collectAsStateWithLifecycle()
                    var activeError by remember { mutableStateOf<ErrorUiModel?>(null) }

                    LaunchedEffect(appAuthViewModel) {
                        appAuthViewModel.effects.collect { effect ->
                            when (effect) {
                                is AppAuthEffect.ShowError -> {
                                    activeError = errorPresentationMapper.map(effect.error)
                                }
                            }
                        }
                    }

                    when (val state = appAuthUiState) {
                        AppAuthUiState.Loading -> Unit
                        is AppAuthUiState.Ready -> {
                            val startDestination = remember(state.authState) {
                                startDestinationForAuthState(
                                    authState = state.authState,
                                    activeProfileId = state.activeProfileId,
                                )
                            }

                            StreamCoreNavHost(
                                startDestination = startDestination,
                                authState = state.authState,
                                isLogoutConfirmationVisible = state.isLogoutConfirmationVisible,
                                isLogoutInProgress = state.isLogoutInProgress,
                                onActiveProfileChanged = appAuthViewModel::onActiveProfileChanged,
                                onLogoutRequested = {
                                    appAuthViewModel.onAction(AppAuthAction.RequestLogout)
                                },
                                onError = { error ->
                                    activeError = errorPresentationMapper.map(error)
                                },
                            )

                            AppLogoutConfirmationDialog(
                                visible = state.isLogoutConfirmationVisible,
                                isLogoutInProgress = state.isLogoutInProgress,
                                onConfirm = {
                                    appAuthViewModel.onAction(AppAuthAction.ConfirmLogout)
                                },
                                onDismiss = {
                                    appAuthViewModel.onAction(
                                        AppAuthAction.DismissLogoutConfirmation,
                                    )
                                },
                            )
                        }
                    }

                    ErrorHost(
                        presentation = activeError,
                        onDismiss = { activeError = null },
                    )
                }
            }
        }
    }

    private fun configureTvSoftInputMode() {
        val uiModeType = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        if (uiModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
        }
    }
}
