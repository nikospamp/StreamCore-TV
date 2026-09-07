package com.pampoukidis.streamcoretv.feature.login.common.login

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens

/**
 * Spacing for [LoginForm]. Defaults match mobile/tablet; platform callers can override individual values.
 *
 * @property verticalSpacing Gap between fields, the primary action, the secondary-action row, and help.
 * @property submitTopPadding Extra space above Continue, added to [verticalSpacing].
 * @property secondaryActionsSpacing Horizontal gap between Forgot password and Create account.
 * @property secondaryActionsTopPadding Extra space above the secondary-action row, added to [verticalSpacing].
 */
@Immutable
data class LoginFormLayout(
    val verticalSpacing: Dp = StreamCoreDimens.Spacing.Small,
    val submitTopPadding: Dp = StreamCoreDimens.Spacing.ExtraLarge,
    val secondaryActionsSpacing: Dp = StreamCoreDimens.Spacing.Tiny,
    val secondaryActionsTopPadding: Dp = StreamCoreDimens.Spacing.Tiny,
)
