package com.pampoukidis.streamcoretv.feature.login.tablet.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginBackground
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginBackgroundVariant
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginMaterialForm
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.core.ui.R

@Composable
fun TabletLoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LoginBackground(
        variant = LoginBackgroundVariant.Landscape,
        modifier = modifier.testTag(LoginTestTags.Root),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier.widthIn(min = 360.dp, max = 460.dp),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                    )
                    LoginMaterialForm(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@PreviewTablet
@Composable
private fun TabletLoginScreenPreview() {
    StreamCoreTVTheme {
        TabletLoginScreen(
            state = LoginUiState(
                email = "lead@streamcore.tv",
                password = "password",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}
