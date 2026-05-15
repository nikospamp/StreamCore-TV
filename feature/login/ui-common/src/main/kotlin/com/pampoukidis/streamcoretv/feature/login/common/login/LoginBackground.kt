package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pampoukidis.streamcoretv.core.ui.R

enum class LoginBackgroundVariant {
    Portrait,
    Landscape,
}

@Composable
fun LoginBackground(
    variant: LoginBackgroundVariant,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(
                id = when (variant) {
                    LoginBackgroundVariant.Portrait -> R.drawable.streamcore_login_background_small
                    LoginBackgroundVariant.Landscape -> R.drawable.streamcore_login_background_large
                },
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        content()
    }
}
