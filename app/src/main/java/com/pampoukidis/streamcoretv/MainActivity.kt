package com.pampoukidis.streamcoretv

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pampoukidis.streamcoretv.auth.AppAuthEffect
import com.pampoukidis.streamcoretv.auth.AppAuthUiState
import com.pampoukidis.streamcoretv.auth.AppAuthViewModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.components.ErrorHost
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.navigation.StreamCoreNavHost
import com.pampoukidis.streamcoretv.navigation.startDestinationForAuthState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var errorPresentationMapper: ErrorPresentationMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTvSoftInputMode()
        enableEdgeToEdge()
        setContent {
            StreamCoreTheme {
                val appAuthViewModel: AppAuthViewModel = hiltViewModel()
                val appAuthUiState by appAuthViewModel.uiState.collectAsStateWithLifecycle()
                var activeError by remember { mutableStateOf<ErrorModel?>(null) }

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
                            onActiveProfileChanged = appAuthViewModel::onActiveProfileChanged,
                            onError = { error ->
                                activeError = errorPresentationMapper.map(error)
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

    private fun configureTvSoftInputMode() {
        val uiModeType = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        if (uiModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
        }
    }
}