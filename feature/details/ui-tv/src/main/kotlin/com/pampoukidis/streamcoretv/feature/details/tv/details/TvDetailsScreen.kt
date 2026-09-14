package com.pampoukidis.streamcoretv.feature.details.tv.details

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTrailerIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvTextButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsActionContent
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsPanoramaHero
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsSynopsis
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsCast
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRecommendationArtwork
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.tv.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    returnFocusKey: String? = null,
    onReturnFocusConsumed: (String) -> Unit = {},
    sharedElementScope: StreamCoreSharedElementScope? = null,
    sourceArtworkUrl: String? = null,
) {
    val (
        retryFocusRequester,
        playFocusRequester,
        likeFocusRequester,
        myListFocusRequester,
        trailerFocusRequester,
        recommendationsFocusRequester,
        actionsFocusRequester,
    ) = remember { FocusRequester.createRefs() }
    val contentId = state.content?.id
    val currentReturnFocusKey by rememberUpdatedState(returnFocusKey)

    LifecycleResumeEffect(contentId, contentId == null && state.isLoading) {
        if (contentId == null && !state.isLoading) {
            retryFocusRequester.requestFocus()
        } else if (contentId != null && currentReturnFocusKey == null) {
            playFocusRequester.requestFocus()
        }
        onPauseOrDispose { }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier
                .fillMaxSize(),
        ) {
            // Avoid TV's default pivot on this screen; the body separately restores the
            // summary when returning from recommendations.
            CompositionLocalProvider(LocalBringIntoViewSpec provides DetailsBringIntoViewSpec) {
                DetailsBody(
                    state = state,
                    onAction = onAction,
                    retryFocusRequester = retryFocusRequester,
                    playFocusRequester = playFocusRequester,
                    actionsFocusRequester = actionsFocusRequester,
                    trailerFocusRequester = trailerFocusRequester,
                    likeFocusRequester = likeFocusRequester,
                    myListFocusRequester = myListFocusRequester,
                    recommendationsFocusRequester = recommendationsFocusRequester,
                    returnFocusKey = returnFocusKey,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                    sharedElementScope = sharedElementScope,
                    sourceArtworkUrl = sourceArtworkUrl,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val DetailsBringIntoViewSpec = object : BringIntoViewSpec {}

private class DetailsVerticalBringIntoViewSpec(
    private val focusClearance: Float,
) : BringIntoViewSpec {
    var viewportCoordinates: LayoutCoordinates? = null
    var heroActionsCoordinates: LayoutCoordinates? = null

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val viewport = viewportCoordinates
        val heroActions = heroActionsCoordinates
        if (viewport?.isAttached == true && heroActions?.isAttached == true && size <= containerSize) {
            val heroActionsBounds = viewport.localBoundingBoxOf(heroActions, clipBounds = false)
            val trailingEdge = offset + size
            if (offset >= heroActionsBounds.top && trailingEdge <= heroActionsBounds.bottom) {
                // Restore the hero/title with focus in the action row. At large font scales,
                // stop earlier when scrolling to zero would hide the requested control/text.
                // Let Compose own the single relocation animation and system motion scale.
                // TV buttons paint their border beyond the focus bounds. Reserve that
                // painted extent both at scroll zero and when the hero and actions cannot fit.
                val clearance = minOf(focusClearance, (containerSize - size) / 2f)
                return maxOf(heroActionsBounds.top, trailingEdge + clearance - containerSize)
            }
        }
        return super.calculateScrollDistance(offset, size, containerSize)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailsBody(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    retryFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    returnFocusKey: String?,
    onReturnFocusConsumed: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope?,
    sourceArtworkUrl: String?,
) {
    val content = state.content
    val scrollState = rememberScrollState()
    val overviewFocusRequester = remember(content?.id) { FocusRequester() }
    val focusClearance = StreamCoreDimens.Tv.Focus.BorderPadding + StreamCoreDimens.Tv.Focus.BorderWidth
    val focusClearancePx = with(LocalDensity.current) { focusClearance.toPx() }
    val verticalBringIntoViewSpec = remember(content?.id, focusClearancePx) {
        DetailsVerticalBringIntoViewSpec(focusClearance = focusClearancePx)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { verticalBringIntoViewSpec.viewportCoordinates = it },
    ) {
        when {
            state.isLoading && content == null -> CircularProgressIndicator()
            content == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            ) {
                Text(
                    text = "Unable to load details.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StreamCoreTvButton(
                    text = "Retry",
                    onClick = { onAction(DetailsAction.Refresh) },
                    enabled = true,
                    shape = StreamCoreControlDefaults.style().buttonShape,
                    contentAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .defaultMinSize(minHeight = StreamCoreDimens.Button.MinHeight)
                        .focusRequester(retryFocusRequester)
                        .testTag(DetailsTestTags.RefreshButton),
                )
            }

            // Keep vertical sections composed so expanded reading content cannot detach
            // any D-pad target. The recommendation rail remains lazy.
            else -> {
                CompositionLocalProvider(LocalBringIntoViewSpec provides verticalBringIntoViewSpec) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DetailsTestTags.Content)
                            .verticalScroll(scrollState)
                            // Clearance scrolls with content; the viewport reaches the screen bottom.
                            .padding(bottom = StreamCoreDimens.Spacing.Large),
                    ) {
                        HeroActionsSection(
                            state = state,
                            onAction = onAction,
                            playFocusRequester = playFocusRequester,
                            actionsFocusRequester = actionsFocusRequester,
                            trailerFocusRequester = trailerFocusRequester,
                            likeFocusRequester = likeFocusRequester,
                            myListFocusRequester = myListFocusRequester,
                            sharedElementScope = sharedElementScope,
                            sourceArtworkUrl = sourceArtworkUrl,
                            overviewFocusRequester = overviewFocusRequester,
                            modifier = Modifier
                                .onGloballyPositioned { verticalBringIntoViewSpec.heroActionsCoordinates = it },
                        )
                        DetailsInformationBand(
                            content = content,
                            overviewFocusRequester = overviewFocusRequester,
                            playFocusRequester = playFocusRequester,
                            downFocusRequester = if (state.recommendations.isNotEmpty()) {
                                recommendationsFocusRequester
                            } else {
                                FocusRequester.Cancel
                            },
                            scrollState = scrollState,
                            viewportCoordinates = { verticalBringIntoViewSpec.viewportCoordinates },
                        )
                        // Horizontal recommendation scrolling retains minimal-visibility behavior.
                        CompositionLocalProvider(LocalBringIntoViewSpec provides DetailsBringIntoViewSpec) {
                            RecommendationsRow(
                                recommendations = state.recommendations,
                                onAction = onAction,
                                recommendationsFocusRequester = recommendationsFocusRequester,
                                actionsUpFocusRequester = playFocusRequester,
                                fallbackFocusRequester = playFocusRequester,
                                returnFocusKey = returnFocusKey,
                                onReturnFocusConsumed = onReturnFocusConsumed,
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun HeroActionsSection(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    sharedElementScope: StreamCoreSharedElementScope?,
    sourceArtworkUrl: String?,
    overviewFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier.fillMaxWidth(),
    ) {
        DetailsPanoramaHero(
            content = content,
            titleStyle = MaterialTheme.typography.headlineLarge,
            metadataStyle = MaterialTheme.typography.labelLarge,
            contentPadding = PaddingValues(
                // Keep the Play button's outer focus ring inside the rounded hero clip.
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Spacing.ExtraLarge,
            ),
            bottomContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    StreamCoreTvButton(
                        text = stringResource(
                            if (state.hasResumableProgress) R.string.details_action_resume else R.string.details_action_play,
                        ),
                        onClick = { onAction(DetailsAction.PlaySelected) },
                        enabled = true,
                        variant = StreamCoreTvButtonVariant.Primary,
                        leadingIcon = { StreamCorePlayIcon() },
                        shape = StreamCoreControlDefaults.style().buttonShape,
                        contentAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(StreamCoreDimens.Tv.Details.PlayButtonWidth)
                            .defaultMinSize(minHeight = StreamCoreDimens.Button.MinHeight)
                            .focusRequester(playFocusRequester)
                            .focusProperties {
                                up = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                                right = if (state.isLibraryAvailable || content.trailers.isNotEmpty()) {
                                    actionsFocusRequester
                                } else {
                                    FocusRequester.Cancel
                                }
                                down = overviewFocusRequester
                            }
                            .testTag(DetailsTestTags.PlayButton),
                    )
                    DetailsActions(
                        hasTrailer = content.trailers.isNotEmpty(),
                        isLibraryAvailable = state.isLibraryAvailable,
                        isLiked = state.isLiked,
                        isInMyList = state.isInMyList,
                        isLikeMutationPending = state.isLikeMutationPending,
                        isMyListMutationPending = state.isMyListMutationPending,
                        playFocusRequester = playFocusRequester,
                        actionsFocusRequester = actionsFocusRequester,
                        trailerFocusRequester = trailerFocusRequester,
                        likeFocusRequester = likeFocusRequester,
                        myListFocusRequester = myListFocusRequester,
                        overviewFocusRequester = overviewFocusRequester,
                        onTrailerClick = { onAction(DetailsAction.TrailerSelected) },
                        onLikeClick = { onAction(DetailsAction.LikeToggled) },
                        onMyListClick = { onAction(DetailsAction.MyListToggled) },
                    )
                }
            },
            sharedElementScope = sharedElementScope,
            sourceArtworkUrl = sourceArtworkUrl,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = StreamCoreDimens.Tv.Details.HeroHeight),
        )
        if (state.isLoading) {
            StreamCoreLoadingChip(
                text = stringResource(R.string.details_action_updating),
                modifier = Modifier.padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
            )
        }
    }
}

@Composable
private fun DetailsInformationBand(
    content: ContentModel,
    overviewFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    scrollState: ScrollState,
    viewportCoordinates: () -> LayoutCoordinates?,
) {
    val castFocusRequester = remember(content.id) { FocusRequester() }
    val hasCast = content.cast.isNotEmpty()

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding)
            .testTag(DetailsTestTags.Overview),
    ) {
        DetailsReadingSection(
            contentId = content.id,
            expandLabel = stringResource(R.string.details_read_full_overview),
            entryFocusRequester = overviewFocusRequester,
            upFocusRequester = playFocusRequester,
            downFocusRequester = downFocusRequester,
            leftFocusRequester = FocusRequester.Cancel,
            rightFocusRequester = if (hasCast) castFocusRequester else FocusRequester.Cancel,
            scrollState = scrollState,
            viewportCoordinates = viewportCoordinates,
            modifier = Modifier.weight(if (hasCast) 1.55f else 1f),
        ) { maxLines, readingModifier ->
            DetailsSynopsis(
                content = content,
                headingStyle = MaterialTheme.typography.titleMedium,
                genresStyle = MaterialTheme.typography.titleSmall,
                descriptionStyle = MaterialTheme.typography.bodyMedium,
                maxLines = maxLines,
                modifier = readingModifier,
            )
        }
        if (hasCast) {
            DetailsReadingSection(
                contentId = content.id,
                expandLabel = stringResource(R.string.details_show_full_cast),
                entryFocusRequester = castFocusRequester,
                upFocusRequester = playFocusRequester,
                downFocusRequester = downFocusRequester,
                leftFocusRequester = overviewFocusRequester,
                rightFocusRequester = FocusRequester.Cancel,
                scrollState = scrollState,
                viewportCoordinates = viewportCoordinates,
                modifier = Modifier.weight(1f),
            ) { maxLines, readingModifier ->
                DetailsCast(
                    content = content,
                    headingStyle = MaterialTheme.typography.titleMedium,
                    castStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = maxLines,
                    modifier = readingModifier,
                )
            }
        }
    }
}

@Composable
private fun DetailsReadingSection(
    contentId: String,
    expandLabel: String,
    entryFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    scrollState: ScrollState,
    viewportCoordinates: () -> LayoutCoordinates?,
    modifier: Modifier = Modifier,
    content: @Composable (maxLines: Int, modifier: Modifier) -> Unit,
) {
    var expanded by rememberSaveable(contentId) { mutableStateOf(false) }
    var isReadingFocused by remember { mutableStateOf(false) }
    var readingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val readingFocusRequester = remember { FocusRequester() }
    val toggleFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusClearance = StreamCoreDimens.Tv.Focus.BorderPadding + StreamCoreDimens.Tv.Focus.BorderWidth
    val focusClearancePx = with(LocalDensity.current) { focusClearance.toPx() }
    val expandedDescription = stringResource(
        if (expanded) R.string.details_text_expanded else R.string.details_text_collapsed,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
        modifier = modifier
            .focusRequester(entryFocusRequester)
            .focusGroup(),
    ) {
        content(
            if (expanded) Int.MAX_VALUE else CollapsedReadingLines,
            Modifier
                .fillMaxWidth()
                .border(
                    width = StreamCoreDimens.Tv.Focus.BorderWidth,
                    color = if (isReadingFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.transparentContainer,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(StreamCoreDimens.Spacing.Small)
                .onGloballyPositioned { readingCoordinates = it }
                .focusRequester(readingFocusRequester)
                .onFocusChanged { isReadingFocused = it.isFocused }
                .focusProperties {
                    up = upFocusRequester
                    down = toggleFocusRequester
                    left = leftFocusRequester
                    right = rightFocusRequester
                }
                .onPreviewKeyEvent { event ->
                    if (!isReadingFocused || (event.key != Key.DirectionUp && event.key != Key.DirectionDown)) {
                        return@onPreviewKeyEvent false
                    }
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent true
                    }
                    val viewport = viewportCoordinates()
                    val reading = readingCoordinates
                    if (viewport?.isAttached != true || reading?.isAttached != true) {
                        return@onPreviewKeyEvent false
                    }
                    val bounds = viewport.localBoundingBoxOf(reading, clipBounds = false)
                    val viewportHeight = viewport.size.height.toFloat()
                    val readingStep = viewportHeight * ReadingScrollFraction
                    val distance = if (event.key == Key.DirectionDown) {
                        minOf(readingStep, maxOf(0f, bounds.bottom + focusClearancePx - viewportHeight))
                    } else {
                        -minOf(readingStep, maxOf(0f, focusClearancePx - bounds.top))
                    }
                    val canScroll = when {
                        distance > 1f -> scrollState.canScrollForward
                        distance < -1f -> scrollState.canScrollBackward
                        else -> false
                    }
                    if (canScroll) {
                        coroutineScope.launch {
                            scrollState.scroll(MutatePriority.UserInput) { scrollBy(distance) }
                        }
                    } else {
                        val target = if (event.key == Key.DirectionDown) toggleFocusRequester else upFocusRequester
                        if (target != FocusRequester.Cancel) {
                            target.requestFocus()
                        }
                    }
                    true
                }
                .focusable(enabled = expanded),
        )
        StreamCoreTvTextButton(
            text = if (expanded) stringResource(R.string.details_show_less) else expandLabel,
            enabled = true,
            onClick = {
                expanded = !expanded
                if (expanded) {
                    coroutineScope.launch {
                        // Measure full text before focusing it, then start reading at its top.
                        withFrameNanos { }
                        readingFocusRequester.requestFocusWhenReady()
                        val viewport = viewportCoordinates()
                        val reading = readingCoordinates
                        if (viewport?.isAttached == true && reading?.isAttached == true) {
                            val top = viewport.localBoundingBoxOf(reading, clipBounds = false).top
                            scrollState.scroll(MutatePriority.UserInput) { scrollBy(top - focusClearancePx) }
                        }
                    }
                }
            },
            modifier = Modifier
                .focusRequester(toggleFocusRequester)
                .focusProperties {
                    up = if (expanded) readingFocusRequester else upFocusRequester
                    down = downFocusRequester
                    left = leftFocusRequester
                    right = rightFocusRequester
                }
                .semantics { stateDescription = expandedDescription },
        )
    }
}

private const val ReadingScrollFraction = 0.6f
private const val CollapsedReadingLines = 3

@Composable
private fun DetailsActions(
    hasTrailer: Boolean,
    isLibraryAvailable: Boolean,
    isLiked: Boolean,
    isInMyList: Boolean,
    isLikeMutationPending: Boolean,
    isMyListMutationPending: Boolean,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    overviewFocusRequester: FocusRequester,
    onTrailerClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMyListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstActionFocusRequester = when {
        isLibraryAvailable -> likeFocusRequester
        hasTrailer -> trailerFocusRequester
        else -> overviewFocusRequester
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .focusRequester(actionsFocusRequester)
            .focusRestorer(fallback = firstActionFocusRequester)
            .focusGroup(),
    ) {
        DetailsLabeledAction(
            label = stringResource(
                if (isLiked) R.string.details_action_liked else R.string.details_action_like,
            ),
            selectedStateDescription = stringResource(
                if (isLiked) R.string.details_like_selected else R.string.details_like_unselected,
            ),
            selected = isLiked,
            isAvailable = isLibraryAvailable,
            isLoading = isLikeMutationPending,
            onClick = onLikeClick,
            modifier = Modifier
                .widthIn(min = StreamCoreDimens.Button.MinHeight + StreamCoreDimens.Spacing.Large)
                .focusRequester(likeFocusRequester)
                .focusProperties {
                    canFocus = isLibraryAvailable
                    up = FocusRequester.Cancel
                    left = playFocusRequester
                    right = myListFocusRequester
                    down = overviewFocusRequester
                }
                .testTag(DetailsTestTags.LikeAction),
        ) {
            StreamCoreHeartIcon(filled = isLiked)
        }
        DetailsLabeledAction(
            label = stringResource(R.string.details_action_my_list),
            selectedStateDescription = stringResource(
                if (isInMyList) R.string.details_my_list_selected else R.string.details_my_list_unselected,
            ),
            selected = isInMyList,
            isAvailable = isLibraryAvailable,
            isLoading = isMyListMutationPending,
            onClick = onMyListClick,
            modifier = Modifier
                .widthIn(min = StreamCoreDimens.Button.MinHeight + StreamCoreDimens.Spacing.Large)
                .focusRequester(myListFocusRequester)
                .focusProperties {
                    canFocus = isLibraryAvailable
                    up = FocusRequester.Cancel
                    left = likeFocusRequester
                    right = if (hasTrailer) trailerFocusRequester else FocusRequester.Cancel
                    down = overviewFocusRequester
                }
                .testTag(DetailsTestTags.MyListAction),
        ) {
            StreamCoreBookmarkIcon(filled = isInMyList)
        }
        if (hasTrailer) {
            DetailsLabeledAction(
                label = stringResource(R.string.details_action_trailer),
                selectedStateDescription = "",
                selected = null,
                isAvailable = true,
                isLoading = false,
                onClick = onTrailerClick,
                modifier = Modifier
                    .widthIn(min = StreamCoreDimens.Button.MinHeight + StreamCoreDimens.Spacing.Large)
                    .focusRequester(trailerFocusRequester)
                    .focusProperties {
                        up = FocusRequester.Cancel
                        left = if (isLibraryAvailable) myListFocusRequester else playFocusRequester
                        right = FocusRequester.Cancel
                        down = overviewFocusRequester
                    }
                    .testTag(DetailsTestTags.TrailerAction),
            ) {
                StreamCoreTrailerIcon()
            }
        }
    }
}

@Composable
private fun DetailsLabeledAction(
    label: String,
    selectedStateDescription: String,
    selected: Boolean?,
    isAvailable: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val actionStateDescription = when {
        !isAvailable -> stringResource(R.string.details_action_not_available)
        isLoading -> stringResource(R.string.details_action_updating)
        else -> selectedStateDescription
    }
    val contentColor = when {
        !isAvailable -> MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.38f)
        selected == true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onArtwork
    }

    Surface(
        shape = shape,
        color = if (isFocused) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        border = if (isFocused) {
            BorderStroke(StreamCoreDimens.Tv.Focus.BorderWidth, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = modifier
            .sizeIn(
                minWidth = StreamCoreDimens.Button.MinHeight,
                minHeight = StreamCoreDimens.Button.MinHeight,
            )
            .onFocusChanged { isFocused = it.isFocused }
            // Surface's internal clip cannot contain an indication on its caller modifier.
            .clip(shape)
            // Retain D-pad focus during a mutation; semantics and the guard block activation.
            .clickable(
                enabled = isAvailable,
                role = if (selected == null) Role.Button else Role.Checkbox,
                onClick = { if (!isLoading) onClick() },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = actionStateDescription
                if (selected != null) {
                    this.selected = selected
                }
                if (!isAvailable || isLoading) {
                    disabled()
                }
            },
    ) {
        DetailsActionContent(
            label = label,
            contentColor = contentColor,
            isLoading = isLoading,
            labelStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = StreamCoreDimens.Spacing.Small),
            icon = icon,
        )
    }
}

@Composable
private fun RecommendationsRow(
    recommendations: List<ContentModel>,
    onAction: (DetailsAction) -> Unit,
    recommendationsFocusRequester: FocusRequester,
    actionsUpFocusRequester: FocusRequester,
    fallbackFocusRequester: FocusRequester,
    returnFocusKey: String?,
    onReturnFocusConsumed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) {
        LaunchedEffect(returnFocusKey) {
            val focusKey = returnFocusKey ?: return@LaunchedEffect
            if (fallbackFocusRequester.requestFocusWhenReady()) {
                onReturnFocusConsumed(focusKey)
            }
        }
        return
    }

    val rowState = rememberLazyListState()
    val returnFocusRequester = remember { FocusRequester() }
    val returnFocusIndex = remember(recommendations, returnFocusKey) {
        recommendations.indexOfFirst { content ->
            content.sharedContentKey() == returnFocusKey
        }
    }

    LaunchedEffect(returnFocusIndex, returnFocusKey) {
        val focusKey = returnFocusKey ?: return@LaunchedEffect
        val targetRequester = if (returnFocusIndex >= 0) {
            rowState.scrollToItem(returnFocusIndex)
            returnFocusRequester
        } else {
            fallbackFocusRequester
        }
        if (targetRequester.requestFocusWhenReady()) {
            onReturnFocusConsumed(focusKey)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tv.Focus.BorderPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier
                .focusRequester(recommendationsFocusRequester)
                .focusRestorer(),
        ) {
            itemsIndexed(
                items = recommendations,
                key = { _, content -> content.id },
                contentType = { _, _ -> "recommendation" },
            ) { index, content ->
                RecommendationCard(
                    content = content,
                    onClick = {
                        onAction(DetailsAction.RecommendationSelected(content, sourceArtworkUrl = content.poster))
                    },
                    modifier = Modifier
                        .then(
                            if (index == returnFocusIndex) {
                                Modifier.focusRequester(returnFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .focusProperties { up = actionsUpFocusRequester },
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    content: ContentModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) {
            StreamCoreDimens.Elevation.Medium
        } else {
            StreamCoreDimens.Elevation.Low
        },
        border = if (isFocused) {
            BorderStroke(
                width = StreamCoreDimens.Tv.Focus.BorderWidth,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
        modifier = modifier
            .width(StreamCoreDimens.Tv.Details.RecommendationCardWidth)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        DetailsRecommendationArtwork(
            content = content,
            titleStyle = MaterialTheme.typography.labelMedium,
            ratingStyle = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun ContentModel.sharedContentKey(): String {
    return StreamCoreSharedKey.content(
        contentId = id,
        row = row,
    )
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

private const val FocusRequestAttempts = 3

@PreviewTV
@Composable
private fun TvDetailsScreenDefaultPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvDetailsScreenResumeSelectedPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
                hasResumableProgress = true,
                isLibraryAvailable = true,
                isLiked = true,
                isInMyList = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvDetailsScreenRetainedLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(
                isLoading = true,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
                isMyListMutationPending = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvDetailsScreenUnavailableActionsPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = false,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvDetailsScreenLongContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content.copy(
                    title = "Beyond the Horizon: A Journey Through the Unknown",
                    description = DetailsPreviewData.content.description.repeat(4),
                    cast = List(8) { DetailsPreviewData.content.cast }.flatten(),
                ),
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvDetailsScreenLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(isLoading = true),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvDetailsScreenErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvDetailsScreen(
            state = DetailsUiState(isLoading = false),
            onAction = {},
        )
    }
}
