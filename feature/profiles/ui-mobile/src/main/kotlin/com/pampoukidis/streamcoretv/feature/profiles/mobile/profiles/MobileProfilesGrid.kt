package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode

@Composable
internal fun MobileProfilesGrid(
    profiles: List<ProfileModel>,
    mode: ProfilesMode,
    pendingSelectionProfileId: String?,
    interactionsEnabled: Boolean,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = StreamCoreDimens.Spacing.Small,
            bottom = 32.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier,
    ) {
        items(
            items = profiles,
            key = { profile -> profile.id },
            contentType = { "profile" },
        ) { profile ->
            MobileProfileTile(
                profile = profile,
                mode = mode,
                isSelecting = pendingSelectionProfileId == profile.id,
                enabled = interactionsEnabled,
                sharedElementScope = sharedElementScope,
                onClick = {
                    if (mode == ProfilesMode.Selection) {
                        onSelectProfile(profile.id)
                    } else {
                        onEditProfile(profile.id)
                    }
                },
            )
        }
        item(
            key = "add-profile",
            contentType = "add-profile",
            span = {
                GridItemSpan(if (profiles.isEmpty()) maxLineSpan else 1)
            },
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MobileAddProfileTile(
                    enabled = interactionsEnabled,
                    onClick = onCreateProfile,
                )
            }
        }
    }
}
