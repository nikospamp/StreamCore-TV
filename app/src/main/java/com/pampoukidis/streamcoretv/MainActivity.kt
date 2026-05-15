package com.pampoukidis.streamcoretv

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorModel
import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import com.pampoukidis.streamcoretv.core.ui.components.ErrorHost
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.LoginPlatform
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform
import com.pampoukidis.streamcoretv.feature.login.mobile.MobileLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tablet.TabletLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tv.TvLoginRoute
import com.pampoukidis.streamcoretv.feature.profiles.mobile.MobileProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tablet.TabletProfilesRoute
import com.pampoukidis.streamcoretv.feature.profiles.tv.TvProfilesRoute
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
            StreamCoreTVTheme {
                var activeError by remember { mutableStateOf<ErrorModel?>(null) }
                var activeProfile by remember { mutableStateOf<ProfileModel?>(null) }
                var authStep by remember { mutableStateOf(AuthStep.Login) }

                when (authStep) {
                    AuthStep.Login -> LoginApp(
                        onLoginSucceeded = { authStep = AuthStep.ProfileSelection },
                        onForgotPassword = {},
                        onCreateAccount = {},
                        onHelp = {},
                        onError = { error ->
                            activeError = errorPresentationMapper.map(error)
                        },
                    )

                    AuthStep.ProfileSelection -> ProfilesApp(
                        onProfileSelected = { profile ->
                            activeProfile = profile
                            authStep = AuthStep.Authenticated
                        },
                        onError = { error ->
                            activeError = errorPresentationMapper.map(error)
                        },
                    )

                    AuthStep.Authenticated -> AuthenticatedPlaceholder(
                        profile = activeProfile,
                    )
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

private enum class AuthStep {
    Login,
    ProfileSelection,
    Authenticated,
}

@Composable
private fun LoginApp(
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
private fun ProfilesApp(
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
    profile: ProfileModel?,
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
                text = "Active profile: ${profile?.displayName.orEmpty()}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}