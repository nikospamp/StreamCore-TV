package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode

@Composable
internal fun TvProfilesRow(
    profiles: List<ProfileModel>,
    mode: ProfilesMode,
    pendingSelectionProfileId: String?,
    interactionsEnabled: Boolean,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocusRequester = remember { FocusRequester() }
    val firstProfileId = profiles.firstOrNull()?.id

    LaunchedEffect(firstProfileId, interactionsEnabled) {
        if (interactionsEnabled) {
            initialFocusRequester.requestFocus()
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = StreamCoreDimens.Spacing.ExtraLarge),
        horizontalArrangement = Arrangement.spacedBy(
            space = StreamCoreDimens.Spacing.ExtraLarge,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        itemsIndexed(
            items = profiles,
            key = { _, profile -> profile.id },
            contentType = { _, _ -> "profile" },
        ) { index, profile ->
            TvProfileTile(
                profile = profile,
                mode = mode,
                isSelecting = pendingSelectionProfileId == profile.id,
                enabled = interactionsEnabled,
                onClick = {
                    if (mode == ProfilesMode.Selection) {
                        onSelectProfile(profile.id)
                    } else {
                        onEditProfile(profile.id)
                    }
                },
                modifier = if (index == 0) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
        item(
            key = "add-profile",
            contentType = "add-profile",
        ) {
            TvAddProfileTile(
                enabled = interactionsEnabled,
                onClick = onCreateProfile,
                modifier = if (profiles.isEmpty()) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}
