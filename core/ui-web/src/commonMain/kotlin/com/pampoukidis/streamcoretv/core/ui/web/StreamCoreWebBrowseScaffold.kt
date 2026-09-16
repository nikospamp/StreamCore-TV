package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
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
    profile: ProfileModel? = null,
    content: @Composable () -> Unit,
) {
    val contentFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(start = if (activeDestination == WebBrowseDestination.Home) 0.dp else StreamCoreDimens.Tv.Navigation.CollapsedWidth)
                .focusRequester(contentFocus).focusRestorer().focusGroup(),
        ) {
            content()
        }
        StreamCoreWebNavigationRail(
            activeDestination = activeDestination,
            profileName = profileName,
            profile = profile,
            enabled = !logoutInProgress,
            allowExpansion = maxWidth >= StreamCoreWebDimens.NavigationExpansionMinWidth,
            onDestinationSelected = { destination ->
                onDestinationSelected(destination)
                if (destination == activeDestination) contentFocus.requestFocus() else focusManager.clearFocus()
            },
            onChangeProfile = onChangeProfile,
            onMoveToContent = { contentFocus.requestFocus() },
            modifier = Modifier.align(Alignment.CenterStart),
        )
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
            Text("Browse content", style = MaterialTheme.typography.displaySmall)
        }
    }
}
