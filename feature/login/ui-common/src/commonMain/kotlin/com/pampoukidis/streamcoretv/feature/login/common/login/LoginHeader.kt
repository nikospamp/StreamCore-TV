package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.jetbrains.compose.resources.stringResource
import streamcoretv.core.ui.generated.resources.Res
import streamcoretv.core.ui.generated.resources.login_subtitle
import streamcoretv.core.ui.generated.resources.login_title

@Composable
fun LoginHeader(
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    titleModifier: Modifier = Modifier,
    subtitleModifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.login_title),
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = titleModifier.semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = subtitleModifier,
        )
    }
}

@Preview
@Composable
private fun LoginHeaderPreview() {
    StreamCoreTheme {
        LoginHeader()
    }
}