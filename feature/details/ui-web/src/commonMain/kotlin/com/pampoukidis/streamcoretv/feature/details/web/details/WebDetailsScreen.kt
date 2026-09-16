package com.pampoukidis.streamcoretv.feature.details.web.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTrailerIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebActionSurface
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebContentCard
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsActionContent
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsCast
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsPanoramaHero
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRecommendationArtwork
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsSynopsis
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.web.testing.WebDetailsFixtures
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.details.ui_web.generated.resources.Res
import streamcoretv.feature.details.ui_web.generated.resources.web_details_action_unavailable
import streamcoretv.feature.details.ui_web.generated.resources.web_details_action_updating
import streamcoretv.feature.details.ui_web.generated.resources.web_details_read_full_overview
import streamcoretv.feature.details.ui_web.generated.resources.web_details_show_full_cast
import streamcoretv.feature.details.ui_web.generated.resources.web_details_show_less
import streamcoretv.feature.details.ui_web.generated.resources.web_details_text_expanded
import streamcoretv.feature.details.ui_web.generated.resources.web_details_text_collapsed
import streamcoretv.feature.details.ui_web.generated.resources.web_details_back
import streamcoretv.feature.details.ui_web.generated.resources.web_details_error_message
import streamcoretv.feature.details.ui_web.generated.resources.web_details_error_title
import streamcoretv.feature.details.ui_web.generated.resources.web_details_in_my_list
import streamcoretv.feature.details.ui_web.generated.resources.web_details_like
import streamcoretv.feature.details.ui_web.generated.resources.web_details_liked
import streamcoretv.feature.details.ui_web.generated.resources.web_details_loading
import streamcoretv.feature.details.ui_web.generated.resources.web_details_my_list
import streamcoretv.feature.details.ui_web.generated.resources.web_details_no_recommendations
import streamcoretv.feature.details.ui_web.generated.resources.web_details_play
import streamcoretv.feature.details.ui_web.generated.resources.web_details_recommendations
import streamcoretv.feature.details.ui_web.generated.resources.web_details_resume
import streamcoretv.feature.details.ui_web.generated.resources.web_details_retry
import streamcoretv.feature.details.ui_web.generated.resources.web_details_trailer
import streamcoretv.feature.details.ui_web.generated.resources.web_details_updating

@Composable
fun WebDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val trailerFocusRequester = remember { FocusRequester() }
    val likeFocusRequester = remember { FocusRequester() }
    val myListFocusRequester = remember { FocusRequester() }
    val recommendationsState = rememberLazyListState()
    val recommendationFocusRequesters = remember(state.recommendations) {
        List(state.recommendations.size) { FocusRequester() }
    }
    val exactFocusTarget = remember(state, returnFocusKey) {
        state.findWebDetailsFocusTarget(returnFocusKey)
    }
    val initialFocusTarget = remember(state.content, state.isLoading) {
        state.initialWebDetailsFocusTarget()
    }
    var focusAssigned by remember { mutableStateOf(false) }
    val currentOnReturnFocusConsumed by rememberUpdatedState(onReturnFocusConsumed)

    LaunchedEffect(returnFocusKey) {
        if (returnFocusKey != null) {
            focusAssigned = false
        }
    }

    val requestedFocusTarget = if (focusAssigned) {
        null
    } else {
        exactFocusTarget ?: initialFocusTarget
    }

    LaunchedEffect(
        requestedFocusTarget,
        exactFocusTarget,
        state.content?.id,
        state.isLoading,
        recommendationFocusRequesters,
    ) {
        val target = requestedFocusTarget ?: return@LaunchedEffect
        val targetRequester = when (target.sectionKey) {
            WebDetailsRecommendationsSection -> {
                val itemIndex = target.itemIndex ?: return@LaunchedEffect
                if (itemIndex !in recommendationFocusRequesters.indices) {
                    return@LaunchedEffect
                }
                recommendationsState.scrollToItem(itemIndex)
                recommendationFocusRequesters[itemIndex]
            }

            WebDetailsActionsSection -> when (target.itemKey) {
                WebDetailsBackItem -> backFocusRequester
                WebDetailsRefreshItem -> backFocusRequester
                WebDetailsPlayItem -> playFocusRequester
                WebDetailsTrailerItem -> trailerFocusRequester
                WebDetailsLikeItem -> likeFocusRequester
                WebDetailsMyListItem -> myListFocusRequester
                else -> return@LaunchedEffect
            }

            else -> return@LaunchedEffect
        }

        if (targetRequester.requestFocusWhenReady()) {
            val consumedKey = returnFocusKey?.takeIf { exactFocusTarget == target }
            if (consumedKey != null) {
                focusAssigned = true
                currentOnReturnFocusConsumed(consumedKey)
            } else if (returnFocusKey == null && !state.isLoading) {
                focusAssigned = true
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val backgroundEndColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val backgroundBrush = remember(backgroundColor, backgroundEndColor) {
        Brush.verticalGradient(colors = listOf(backgroundColor, backgroundEndColor))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(rememberScrollState())
            .webEscape { onAction(DetailsAction.BackSelected) }
            .testTag(DetailsTestTags.Root),
    ) {
        if (state.content == null) {
            WebDetailsHeader(
                state = state,
                backFocusRequester = backFocusRequester,
                retryFocusRequester = retryFocusRequester,
                playFocusRequester = playFocusRequester,
                onAction = onAction,
            )
        }

        when {
            state.isLoading && state.content == null -> WebDetailsLoading()
            state.content == null -> WebDetailsError(
                retryFocusRequester = retryFocusRequester,
                backFocusRequester = backFocusRequester,
                onAction = onAction,
            )

            else -> WebDetailsContent(
                state = state,
                playFocusRequester = playFocusRequester,
                trailerFocusRequester = trailerFocusRequester,
                likeFocusRequester = likeFocusRequester,
                myListFocusRequester = myListFocusRequester,
                recommendationFocusRequesters = recommendationFocusRequesters,
                recommendationsState = recommendationsState,
                backFocusRequester = backFocusRequester,
                onAction = onAction,
                topControls = {
                    WebDetailsHeader(
                        state = state,
                        backFocusRequester = backFocusRequester,
                        retryFocusRequester = retryFocusRequester,
                        playFocusRequester = playFocusRequester,
                        onAction = onAction,
                    )
                },
            )
        }
    }
}

@Composable
private fun WebDetailsHeader(
    state: DetailsUiState,
    backFocusRequester: FocusRequester,
    retryFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    onAction: (DetailsAction) -> Unit,
) {
    val downRequester = when {
        state.content != null -> playFocusRequester
        !state.isLoading -> retryFocusRequester
        else -> FocusRequester.Cancel
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding, vertical = StreamCoreDimens.Spacing.Medium),
    ) {
        StreamCoreWebArtworkIconButton(
            contentDescription = stringResource(Res.string.web_details_back),
            onClick = { onAction(DetailsAction.BackSelected) },
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .focusProperties {
                    right = FocusRequester.Cancel
                    down = downRequester
                }
                .testTag(DetailsTestTags.BackButton),
        ) { StreamCoreBackIcon() }
        if (state.isLoading && state.content != null) {
            Text(
                text = stringResource(Res.string.web_details_updating),
                color = MaterialTheme.colorScheme.onArtwork,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun WebDetailsLoading() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Loading),
    ) {
        Text(
            text = stringResource(Res.string.web_details_loading),
            style = MaterialTheme.typography.headlineMedium,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StreamCoreDimens.Tv.Details.HeroHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
        ) {
            repeat(LoadingLineCount) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 0) 0.5f else 0.72f)
                        .height(if (index == 0) StreamCoreDimens.Web.Details.LoadingTitleHeight else StreamCoreDimens.Web.Details.LoadingLineHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
                )
            }
        }
    }
}

@Composable
private fun WebDetailsError(
    retryFocusRequester: FocusRequester,
    backFocusRequester: FocusRequester,
    onAction: (DetailsAction) -> Unit,
) {
    StreamCoreWebPanel(
        modifier = Modifier
            .testTag(DetailsTestTags.Error),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
            Text(
                text = stringResource(Res.string.web_details_error_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.web_details_error_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            StreamCoreWebButton(
                text = stringResource(Res.string.web_details_retry),
                onClick = { onAction(DetailsAction.Refresh) },
                variant = StreamCoreWebButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(retryFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                    }
                    .testTag(DetailsTestTags.RetryButton),
            )
        }
    }
}

@Composable
private fun WebDetailsContent(
    state: DetailsUiState,
    playFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationFocusRequesters: List<FocusRequester>,
    recommendationsState: androidx.compose.foundation.lazy.LazyListState,
    backFocusRequester: FocusRequester,
    onAction: (DetailsAction) -> Unit,
    topControls: @Composable () -> Unit,
) {
    val content = requireNotNull(state.content)
    val recommendationRequester = recommendationFocusRequesters.firstOrNull()
        ?: FocusRequester.Cancel
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Content),
    ) {
        DetailsPanoramaHero(
            content = content,
            titleStyle = MaterialTheme.typography.headlineLarge,
            metadataStyle = MaterialTheme.typography.labelLarge,
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Spacing.ExtraLarge,
            ),
            modifier = Modifier
                .heightIn(min = StreamCoreDimens.Tv.Details.HeroHeight),
            overlay = { topControls() },
            bottomContent = {
                WebDetailsActions(
                    state = state,
                    playFocusRequester = playFocusRequester,
                    trailerFocusRequester = trailerFocusRequester,
                    likeFocusRequester = likeFocusRequester,
                    myListFocusRequester = myListFocusRequester,
                    backFocusRequester = backFocusRequester,
                    recommendationsFocusRequester = recommendationRequester,
                    onAction = onAction,
                )
                if (!state.isLibraryAvailable) {
                    Text(
                        text = stringResource(Res.string.web_details_action_unavailable),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onArtwork,
                    )
                }
            },
        )
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding)) {
            if (maxWidth < WebDetailsDimens.InformationBreakpoint) {
                Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
                    WebDetailsSynopsis(content, modifier = Modifier.testTag(DetailsTestTags.Overview))
                    WebDetailsCast(content)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge)) {
                    WebDetailsSynopsis(
                        content = content,
                        modifier = Modifier
                            .weight(1.55f)
                            .testTag(DetailsTestTags.Overview),
                    )
                    if (content.cast.isNotEmpty()) {
                        WebDetailsCast(content, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        WebDetailsRecommendations(
            recommendations = state.recommendations,
            listState = recommendationsState,
            focusRequesters = recommendationFocusRequesters,
            upFocusRequester = playFocusRequester,
            onSelected = { recommendation ->
                onAction(DetailsAction.RecommendationSelected(recommendation))
            },
        )
    }
}

@Composable
private fun WebDetailsSynopsis(content: ContentModel, modifier: Modifier = Modifier) {
    WebDetailsReadingSection(
        contentId = content.id,
        expandLabel = stringResource(Res.string.web_details_read_full_overview),
        textKey = content.description,
        modifier = modifier,
    ) { maxLines, onTextLayout ->
        DetailsSynopsis(
            content = content,
            headingStyle = MaterialTheme.typography.titleMedium,
            genresStyle = MaterialTheme.typography.titleSmall,
            descriptionStyle = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            modifier = Modifier.fillMaxWidth().padding(StreamCoreDimens.Spacing.Small),
        )
    }
}

@Composable
private fun WebDetailsCast(content: ContentModel, modifier: Modifier = Modifier) {
    if (content.cast.isEmpty()) return
    WebDetailsReadingSection(
        contentId = content.id,
        expandLabel = stringResource(Res.string.web_details_show_full_cast),
        textKey = content.cast,
        modifier = modifier,
    ) { maxLines, onTextLayout ->
        DetailsCast(
            content = content,
            headingStyle = MaterialTheme.typography.titleMedium,
            castStyle = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            modifier = Modifier.fillMaxWidth().padding(StreamCoreDimens.Spacing.Small),
        )
    }
}

@Composable
private fun WebDetailsReadingSection(
    contentId: String,
    expandLabel: String,
    textKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable (maxLines: Int, onTextLayout: (TextLayoutResult) -> Unit) -> Unit,
) {
    var expanded by rememberSaveable(contentId, textKey) { mutableStateOf(false) }
    var hasCollapsedOverflow by remember(contentId, textKey) { mutableStateOf(false) }
    val expansionState = stringResource(if (expanded) Res.string.web_details_text_expanded else Res.string.web_details_text_collapsed)
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
        modifier = modifier,
    ) {
        content(if (expanded) Int.MAX_VALUE else CollapsedReadingLines) { result ->
            // Expanded measurements must not discard the collapsed overflow result.
            if (!expanded) hasCollapsedOverflow = result.hasVisualOverflow
        }
        if (expanded || hasCollapsedOverflow) {
            StreamCoreWebButton(
                text = if (expanded) stringResource(Res.string.web_details_show_less) else expandLabel,
                onClick = { expanded = !expanded },
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier.semantics { stateDescription = expansionState },
            )
        }
    }
}

@Composable
private fun WebDetailsActions(
    state: DetailsUiState,
    playFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    backFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    onAction: (DetailsAction) -> Unit,
) {
    val hasTrailer = state.content?.trailers?.isNotEmpty() == true
    val likeEnabled = state.isLibraryAvailable && !state.isLikeMutationPending
    val myListEnabled = state.isLibraryAvailable && !state.isMyListMutationPending
    val firstAction = when {
        likeEnabled -> likeFocusRequester
        myListEnabled -> myListFocusRequester
        hasTrailer -> trailerFocusRequester
        else -> FocusRequester.Cancel
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        StreamCoreWebButton(
            text = stringResource(if (state.hasResumableProgress) Res.string.web_details_resume else Res.string.web_details_play),
            onClick = { onAction(DetailsAction.PlaySelected) },
            leadingIcon = { StreamCorePlayIcon() },
            modifier = Modifier
                .padding(end = StreamCoreDimens.Spacing.Small)
                .width(StreamCoreDimens.Tv.Details.PlayButtonWidth)
                .align(Alignment.CenterVertically)
                .focusRequester(playFocusRequester)
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = firstAction
                    up = backFocusRequester
                    down = recommendationsFocusRequester
                }
                .testTag(DetailsTestTags.PlayButton),
        )
        WebDetailsIconAction(
            text = stringResource(if (state.isLiked) Res.string.web_details_liked else Res.string.web_details_like),
            selected = state.isLiked,
            available = state.isLibraryAvailable,
            loading = state.isLikeMutationPending,
            focusRequester = likeFocusRequester,
            leftFocusRequester = playFocusRequester,
            rightFocusRequester = if (myListEnabled) myListFocusRequester else if (hasTrailer) trailerFocusRequester else FocusRequester.Cancel,
            upFocusRequester = backFocusRequester,
            downFocusRequester = recommendationsFocusRequester,
            testTag = DetailsTestTags.LikeAction,
            onClick = { onAction(DetailsAction.LikeToggled) },
            icon = { StreamCoreHeartIcon(filled = state.isLiked) },
        )
        WebDetailsIconAction(
            text = stringResource(if (state.isInMyList) Res.string.web_details_in_my_list else Res.string.web_details_my_list),
            selected = state.isInMyList,
            available = state.isLibraryAvailable,
            loading = state.isMyListMutationPending,
            focusRequester = myListFocusRequester,
            leftFocusRequester = if (likeEnabled) likeFocusRequester else playFocusRequester,
            rightFocusRequester = if (hasTrailer) trailerFocusRequester else FocusRequester.Cancel,
            upFocusRequester = backFocusRequester,
            downFocusRequester = recommendationsFocusRequester,
            testTag = DetailsTestTags.MyListAction,
            onClick = { onAction(DetailsAction.MyListToggled) },
            icon = { StreamCoreBookmarkIcon(filled = state.isInMyList) },
        )
        if (hasTrailer) {
            WebDetailsIconAction(
                text = stringResource(Res.string.web_details_trailer),
                selected = false,
                available = true,
                loading = false,
                focusRequester = trailerFocusRequester,
                leftFocusRequester = if (myListEnabled) myListFocusRequester else if (likeEnabled) likeFocusRequester else playFocusRequester,
                rightFocusRequester = FocusRequester.Cancel,
                upFocusRequester = backFocusRequester,
                downFocusRequester = recommendationsFocusRequester,
                testTag = DetailsTestTags.TrailerAction,
                onClick = { onAction(DetailsAction.TrailerSelected) },
                toggle = false,
                icon = { StreamCoreTrailerIcon() },
            )
        }
    }
}

@Composable
private fun WebDetailsIconAction(
    text: String,
    selected: Boolean,
    available: Boolean,
    loading: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    testTag: String,
    onClick: () -> Unit,
    toggle: Boolean = true,
    icon: @Composable () -> Unit,
) {
    val stateText = when {
        !available -> stringResource(Res.string.web_details_action_unavailable)
        loading -> stringResource(Res.string.web_details_action_updating)
        else -> text
    }
    val contentColor = when {
        !available -> MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.55f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onArtwork
    }
    StreamCoreWebActionSurface(
        onClick = onClick,
        enabled = available && !loading,
        shape = MaterialTheme.shapes.small,
        role = if (toggle) Role.Checkbox else Role.Button,
        containerColor = MaterialTheme.colorScheme.transparentContainer,
        contentColor = contentColor,
        modifier = Modifier
            .widthIn(min = StreamCoreDimens.Button.MinHeight + StreamCoreDimens.Spacing.Large)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = available && !loading
                left = leftFocusRequester
                right = rightFocusRequester
                up = upFocusRequester
                down = downFocusRequester
            }
            .semantics(mergeDescendants = true) {
                role = if (toggle) Role.Checkbox else Role.Button
                if (toggle) this.selected = selected
                stateDescription = stateText
                if (!available || loading) disabled()
            }
            .testTag(testTag),
    ) {
        DetailsActionContent(
            label = text,
            contentColor = contentColor,
            isLoading = loading,
            labelStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(vertical = StreamCoreDimens.Spacing.Small),
            icon = icon,
        )
    }
}

@Composable
private fun WebDetailsRecommendations(
    recommendations: List<ContentModel>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    focusRequesters: List<FocusRequester>,
    upFocusRequester: FocusRequester,
    onSelected: (ContentModel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = stringResource(Res.string.web_details_recommendations),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
        )
        if (recommendations.isEmpty()) {
            Text(
                text = stringResource(Res.string.web_details_no_recommendations),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding, vertical = StreamCoreDimens.Tv.Focus.BorderPadding),
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                itemsIndexed(
                    items = recommendations,
                    key = { _, content -> content.id },
                    contentType = { _, _ -> RecommendationContentType },
                ) { index, content ->
                    WebDetailsRecommendationCard(
                        content = content,
                        onClick = { onSelected(content) },
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                left = if (index > 0) {
                                    focusRequesters[index - 1]
                                } else {
                                    FocusRequester.Cancel
                                }
                                right = if (index < focusRequesters.lastIndex) {
                                    focusRequesters[index + 1]
                                } else {
                                    FocusRequester.Cancel
                                }
                                up = upFocusRequester
                            }
                            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
                    )
                }
            }
        }
    }
}

@Composable
private fun WebDetailsRecommendationCard(
    content: ContentModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StreamCoreWebContentCard(
        onClick = onClick,
        aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.width(StreamCoreDimens.Tv.Details.RecommendationCardWidth),
    ) {
        DetailsRecommendationArtwork(
            content = content,
            titleStyle = MaterialTheme.typography.labelMedium,
            ratingStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

private suspend fun FocusRequester.requestFocusWhenReady(): Boolean {
    repeat(FocusRequestAttempts) {
        withFrameNanos { }
        if (requestFocus()) {
            return true
        }
    }
    return false
}

private const val CollapsedReadingLines = 3
private const val LoadingLineCount = 5
private const val FocusRequestAttempts = 3
private const val RecommendationContentType = "details-recommendation"

@Preview(name = "Details · Loading", widthDp = 1280, heightDp = 720)
@Composable
private fun WebDetailsLoadingPreview() {
    WebDetailsPreview(WebBrowseFixtureScenario.Loading)
}

@Preview(name = "Details · Content", widthDp = 1280, heightDp = 720)
@Composable
private fun WebDetailsContentPreview() {
    WebDetailsPreview(WebBrowseFixtureScenario.Content)
}

@Preview(name = "Details · Empty", widthDp = 1280, heightDp = 720)
@Composable
private fun WebDetailsEmptyPreview() {
    WebDetailsPreview(WebBrowseFixtureScenario.Empty)
}

@Preview(name = "Details · Offline", widthDp = 1280, heightDp = 720)
@Composable
private fun WebDetailsOfflinePreview() {
    WebDetailsPreview(WebBrowseFixtureScenario.Offline)
}

@Preview(name = "Details · Error", widthDp = 1280, heightDp = 720)
@Composable
private fun WebDetailsErrorPreview() {
    WebDetailsPreview(WebBrowseFixtureScenario.Error)
}

@Preview(name = "Details · Long text", widthDp = 1280, heightDp = 720)
@Composable
private fun WebDetailsLongTextPreview() {
    WebDetailsPreview(WebBrowseFixtureScenario.LongText)
}

@Composable
private fun WebDetailsPreview(scenario: WebBrowseFixtureScenario) {
    StreamCoreTheme(darkTheme = true) {
        WebDetailsScreen(
            state = WebDetailsFixtures.state(scenario),
            onAction = {},
            returnFocusKey = null,
            onReturnFocusConsumed = {},
        )
    }
}
