package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens

/** The dialog owner chooses width, height and platform-specific focus controls. */
@Immutable
data class AvatarPickerLayout(
    val columns: Int = 4,
    val gridMaxHeight: Dp = StreamCoreDimens.Mobile.Profiles.AvatarPickerGridMaxHeight,
    val itemSize: Dp = StreamCoreDimens.Mobile.Profiles.AvatarPickerItemSize,
    val selectedBadgeSize: Dp = StreamCoreDimens.Mobile.Profiles.SelectedBadgeSize,
    val badgeOffset: Dp = StreamCoreDimens.Mobile.Profiles.BadgeOffset,
    val horizontalSpacing: Dp = StreamCoreDimens.Spacing.Small,
    val verticalSpacing: Dp = StreamCoreDimens.Spacing.Medium,
)
