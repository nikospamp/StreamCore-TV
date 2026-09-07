package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.ic_visibility_24
import streamcoretv.core.ui.generated.resources.ic_visibility_off_24
import streamcoretv.core.ui.generated.resources.login_password_hide
import streamcoretv.core.ui.generated.resources.login_password_show

@Composable
fun LoginPasswordVisibilityIcon(
    isPasswordVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(
            resource = if (isPasswordVisible) {
                Res.drawable.ic_visibility_24
            } else {
                Res.drawable.ic_visibility_off_24
            },
        ),
        contentDescription = stringResource(
            if (isPasswordVisible) {
                Res.string.login_password_hide
            } else {
                Res.string.login_password_show
            },
        ),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun LoginPasswordVisibilityIconPreview() {
    StreamCoreTheme {
        LoginPasswordVisibilityIcon(isPasswordVisible = false)
    }
}