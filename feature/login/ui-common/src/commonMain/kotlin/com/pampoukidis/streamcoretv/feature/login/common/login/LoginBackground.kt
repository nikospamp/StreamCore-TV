package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.*
import com.pampoukidis.streamcoretv.feature.login.data.LoginBackgroundVariant

@Composable
fun LoginBackground(
    variant: LoginBackgroundVariant,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(
                resource = when (variant) {
                    LoginBackgroundVariant.Portrait -> Res.drawable.streamcore_login_background_small
                    LoginBackgroundVariant.Landscape -> Res.drawable.streamcore_login_background_large
                },
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        content()
    }
}
