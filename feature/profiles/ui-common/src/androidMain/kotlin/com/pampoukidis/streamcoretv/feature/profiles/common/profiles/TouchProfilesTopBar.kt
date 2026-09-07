package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun TouchProfilesTopBar(
    mode: ProfilesMode,
    showManageAction: Boolean,
    showLogoutAction: Boolean,
    profileActionEnabled: Boolean,
    logoutEnabled: Boolean,
    onLogoutRequested: () -> Unit,
    onAction: (ProfilesAction) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
    height: Dp = StreamCoreDimens.Mobile.Profiles.HeaderHeight,
    sideClearance: Dp = StreamCoreDimens.Mobile.Profiles.HeaderSideClearance,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = horizontalPadding),
    ) {
        Text(
            text = if (mode == ProfilesMode.Selection) {
                "Who's watching?"
            } else {
                "Manage profiles"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = sideClearance,
            ),
        )
        if (showLogoutAction) {
            StreamCoreTextButton(
                text = "Sign out",
                onClick = onLogoutRequested,
                enabled = logoutEnabled,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag(ProfilesTestTags.SignOutButton),
            )
        }
        if (showManageAction) {
            StreamCoreTextButton(
                text = if (mode == ProfilesMode.Selection) "Manage" else "Done",
                onClick = {
                    onAction(
                        if (mode == ProfilesMode.Selection) {
                            ProfilesAction.ManageProfiles
                        } else {
                            ProfilesAction.DoneManaging
                        },
                    )
                },
                enabled = profileActionEnabled,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag(ProfilesTestTags.ManageProfilesButton),
            )
        }
    }
}

@Preview
@Composable
private fun TouchProfilesTopBarPreview() {
    StreamCoreTheme(darkTheme = true) {
        TouchProfilesTopBar(
            mode = ProfilesMode.Selection,
            showManageAction = true,
            showLogoutAction = true,
            profileActionEnabled = true,
            logoutEnabled = true,
            onLogoutRequested = {},
            onAction = {},
        )
    }
}
