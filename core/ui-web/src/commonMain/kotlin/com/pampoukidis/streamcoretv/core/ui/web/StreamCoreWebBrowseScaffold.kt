package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreWebBrowseScaffold(
    activeDestination: WebBrowseDestination?,
    profileName: String,
    logoutInProgress: Boolean,
    onDestinationSelected: (WebBrowseDestination) -> Unit,
    onChangeProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        StreamCoreWebTopChrome(
            activeDestination = activeDestination,
            profileName = profileName,
            logoutInProgress = logoutInProgress,
            onDestinationSelected = onDestinationSelected,
            onChangeProfile = onChangeProfile,
            onLogout = onLogout,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = StreamCoreWebDimens.BrowseContentMaxWidth)
                    .fillMaxSize()
                    .padding(
                        horizontal = StreamCoreWebDimens.ScreenHorizontal,
                        vertical = StreamCoreWebDimens.ScreenVertical,
                    ),
            ) {
                content()
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun StreamCoreWebBrowseScaffoldPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebBrowseScaffold(
            activeDestination = WebBrowseDestination.Home,
            profileName = "Nikos",
            logoutInProgress = false,
            onDestinationSelected = {},
            onChangeProfile = {},
            onLogout = {},
        ) {
            Text(
                text = "Browse content",
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }
}
