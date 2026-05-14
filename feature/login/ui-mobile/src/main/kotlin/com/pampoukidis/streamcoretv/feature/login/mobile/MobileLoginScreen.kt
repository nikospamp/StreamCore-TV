package com.pampoukidis.streamcoretv.feature.login.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginAction
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginBackground
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginBackgroundVariant
import com.pampoukidis.streamcoretv.feature.login.common.presentation.LoginMaterialForm
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginUiState
import com.pampoukidis.streamcoretv.core.ui.R

@Composable
fun MobileLoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LoginBackground(
        variant = LoginBackgroundVariant.Portrait,
        modifier = modifier.testTag(LoginTestTags.Root),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                modifier = Modifier.width(120.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    LoginMaterialForm(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@PreviewMobile
@Composable
private fun MobileLoginScreenPreview() {
    StreamCoreTVTheme {
        MobileLoginScreen(
            state = LoginUiState(
                email = "lead@streamcore.tv",
                password = "password",
                isSubmitEnabled = true,
            ),
            onAction = {},
        )
    }
}