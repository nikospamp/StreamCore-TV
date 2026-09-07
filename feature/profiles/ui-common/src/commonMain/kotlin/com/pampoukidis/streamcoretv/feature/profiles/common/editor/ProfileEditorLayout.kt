package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens

/** Geometry supplied by the owning window surface; defaults preserve the phone editor. */
@Immutable
data class ProfileEditorLayout(
    val headerHeight: Dp = StreamCoreDimens.Mobile.Profiles.HeaderHeight,
    val headerHorizontalPadding: Dp = StreamCoreDimens.Spacing.Small,
    val headerSideWidth: Dp = StreamCoreDimens.Mobile.Profiles.EditorTopBarSideWidth,
    val contentHorizontalPadding: Dp = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
    val contentVerticalPadding: Dp = StreamCoreDimens.Spacing.ExtraLarge,
    val contentSpacing: Dp = StreamCoreDimens.Spacing.ExtraLarge,
    val avatarContainerSize: Dp = StreamCoreDimens.Mobile.Profiles.EditorAvatarContainerSize,
    val avatarSize: Dp = StreamCoreDimens.Mobile.Profiles.EditorAvatarSize,
    val avatarBadgeSize: Dp = StreamCoreDimens.Mobile.Profiles.EditorBadgeSize,
    val avatarBadgeOffset: Dp = StreamCoreDimens.Mobile.Profiles.BadgeOffset,
    val avatarCaption: String = "Tap to change",
)
