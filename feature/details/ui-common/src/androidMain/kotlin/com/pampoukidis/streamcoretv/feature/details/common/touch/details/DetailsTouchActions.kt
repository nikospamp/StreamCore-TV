package com.pampoukidis.streamcoretv.feature.details.common.touch.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreShareIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTrailerIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsActionContent
import com.pampoukidis.streamcoretv.feature.details.common.resources.Res
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_like
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_liked
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_my_list
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_trailer
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_share
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_not_available
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_updating
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_like_selected
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_like_unselected
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_my_list_selected
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_my_list_unselected
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_trailer_open
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_trailer_unavailable
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import org.jetbrains.compose.resources.stringResource

// Android touch interaction is shared by mobile/tablet; TV supplies its own focusable shell.
@Composable
fun DetailsTouchActions(
    isLibraryAvailable: Boolean,
    isLiked: Boolean,
    isInMyList: Boolean,
    isLikeMutationPending: Boolean,
    isMyListMutationPending: Boolean,
    isTrailerAvailable: Boolean,
    onLikeClick: () -> Unit,
    onMyListClick: () -> Unit,
    onTrailerClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier.fillMaxWidth(),
    ) {
        DetailsLabeledAction(
            label = stringResource(
                if (isLiked) Res.string.details_action_liked else Res.string.details_action_like,
            ),
            stateDescription = stringResource(
                if (isLiked) Res.string.details_like_selected else Res.string.details_like_unselected,
            ),
            selected = isLiked,
            enabled = isLibraryAvailable,
            isLoading = isLikeMutationPending,
            onClick = onLikeClick,
            baseContentColor = contentColor,
            selectedContentColor = selectedContentColor,
            modifier = Modifier
                .weight(1f)
                .testTag(DetailsTestTags.LikeAction),
        ) {
            StreamCoreHeartIcon(filled = isLiked)
        }
        DetailsLabeledAction(
            label = stringResource(Res.string.details_action_my_list),
            stateDescription = stringResource(
                if (isInMyList) {
                    Res.string.details_my_list_selected
                } else {
                    Res.string.details_my_list_unselected
                },
            ),
            selected = isInMyList,
            enabled = isLibraryAvailable,
            isLoading = isMyListMutationPending,
            onClick = onMyListClick,
            baseContentColor = contentColor,
            selectedContentColor = selectedContentColor,
            modifier = Modifier
                .weight(1f)
                .testTag(DetailsTestTags.MyListAction),
        ) {
            StreamCoreBookmarkIcon(filled = isInMyList)
        }
        DetailsLabeledAction(
            label = stringResource(Res.string.details_action_trailer),
            stateDescription = stringResource(
                if (isTrailerAvailable) Res.string.details_trailer_open else Res.string.details_trailer_unavailable,
            ),
            selected = null,
            enabled = isTrailerAvailable,
            isLoading = false,
            onClick = onTrailerClick,
            baseContentColor = contentColor,
            selectedContentColor = selectedContentColor,
            modifier = Modifier
                .weight(1f)
                .testTag(DetailsTestTags.TrailerAction),
        ) {
            StreamCoreTrailerIcon()
        }
        DetailsLabeledAction(
            label = stringResource(Res.string.details_action_share),
            stateDescription = stringResource(Res.string.details_action_not_available),
            selected = null,
            enabled = false,
            isLoading = false,
            onClick = {},
            baseContentColor = contentColor,
            selectedContentColor = selectedContentColor,
            modifier = Modifier
                .weight(1f)
                .testTag(DetailsTestTags.ShareAction),
        ) {
            StreamCoreShareIcon()
        }
    }
}

@Composable
private fun DetailsLabeledAction(
    label: String,
    stateDescription: String,
    selected: Boolean?,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    baseContentColor: Color,
    selectedContentColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val contentColor = when {
        !enabled -> baseContentColor.copy(alpha = DisabledActionAlpha)
        selected == true -> selectedContentColor
        else -> baseContentColor
    }
    val interactionModifier = if (selected != null) {
        Modifier.toggleable(
            value = selected,
            enabled = enabled && !isLoading,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
    } else {
        Modifier.clickable(
            enabled = enabled && !isLoading,
            role = Role.Button,
            onClick = onClick,
        )
    }

    val actionStateDescription = when {
        !enabled && selected != null -> stringResource(Res.string.details_action_not_available)
        isLoading -> stringResource(Res.string.details_action_updating)
        else -> stateDescription
    }
    DetailsActionContent(
        label = label,
        contentColor = contentColor,
        isLoading = isLoading,
        modifier = modifier
            .sizeIn(
                minWidth = StreamCoreDimens.Icon.TouchTarget,
                minHeight = StreamCoreDimens.Icon.TouchTarget + StreamCoreDimens.Spacing.ExtraLarge,
            )
            .then(interactionModifier)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                this.stateDescription = actionStateDescription
                if (!enabled || isLoading) {
                    disabled()
                }
            }
            .padding(vertical = StreamCoreDimens.Spacing.Small),
        icon = icon,
    )
}

private const val DisabledActionAlpha = 0.38f

@Preview
@Composable
private fun DetailsTouchActionsPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsTouchActions(
            isLibraryAvailable = true,
            isLiked = true,
            isInMyList = false,
            isLikeMutationPending = false,
            isMyListMutationPending = true,
            isTrailerAvailable = true,
            onLikeClick = {},
            onMyListClick = {},
            onTrailerClick = {},
        )
    }
}
