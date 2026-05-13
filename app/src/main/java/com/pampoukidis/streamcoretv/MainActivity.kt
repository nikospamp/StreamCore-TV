package com.pampoukidis.streamcoretv

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.common.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.common.utils.LoginPlatform
import com.pampoukidis.streamcoretv.common.utils.rememberLoginPlatform
import com.pampoukidis.streamcoretv.feature.login.domain.LoginCredentials
import com.pampoukidis.streamcoretv.feature.login.mobile.MobileLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tablet.TabletLoginRoute
import com.pampoukidis.streamcoretv.feature.login.tv.TvLoginRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTvSoftInputMode()
        enableEdgeToEdge()
        setContent {
            StreamCoreTVTheme {
                LoginApp(
                    onSubmitCredentials = {
                        val email = it.email
                        Toast.makeText(this@MainActivity, "Login: $email", Toast.LENGTH_SHORT).show()
                    },
                    onForgotPassword = {},
                    onCreateAccount = {},
                    onHelp = {},
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
@Composable
private fun LoginApp(
    onSubmitCredentials: (LoginCredentials) -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onHelp: () -> Unit,
) {
    when (rememberLoginPlatform()) {
        LoginPlatform.Mobile -> MobileLoginRoute(
            onSubmitCredentials = onSubmitCredentials,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
        )

        LoginPlatform.Tablet -> TabletLoginRoute(
            onSubmitCredentials = onSubmitCredentials,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
        )

        LoginPlatform.Tv -> TvLoginRoute(
            onSubmitCredentials = onSubmitCredentials,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            onHelp = onHelp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginAppPreview() {
    StreamCoreTVTheme {
        LoginApp(
            onSubmitCredentials = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onHelp = {},
        )
    }
}