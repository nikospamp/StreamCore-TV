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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
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
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTrailerIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsActionContent
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsOverview
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
                .fillMaxSize()
                .padding(top = StreamCoreDimens.Spacing.ExtraLarge),
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
    var summaryCoordinates: LayoutCoordinates? = null

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val viewport = viewportCoordinates
        val summary = summaryCoordinates
        if (viewport?.isAttached == true && summary?.isAttached == true && size <= containerSize) {
            val summaryBounds = viewport.localBoundingBoxOf(summary, clipBounds = false)
            val trailingEdge = offset + size
            if (offset >= summaryBounds.top && trailingEdge <= summaryBounds.bottom) {
                // Restore the hero/title with focus in the summary. At large font scales,
                // stop earlier when scrolling to zero would hide the requested control/text.
                // Let Compose own the single relocation animation and system motion scale.
                // TV buttons paint their border beyond the focus bounds. Reserve that
                // painted extent both at scroll zero and when a tall summary cannot fit.
                val clearance = minOf(focusClearance, (containerSize - size) / 2f)
                return maxOf(summaryBounds.top - focusClearance, trailingEdge + clearance - containerSize)
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

            // There are only two vertical sections. Keep both composed so a long summary
            // cannot detach the recommendations' focus target. The horizontal row stays lazy.
            else -> {
                CompositionLocalProvider(LocalBringIntoViewSpec provides verticalBringIntoViewSpec) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DetailsTestTags.Content)
                            .verticalScroll(scrollState)
                            // Clearance scrolls with content; the viewport reaches the screen bottom.
                            .padding(top = focusClearance, bottom = StreamCoreDimens.Spacing.Large),
                    ) {
                        SummarySection(
                            state = state,
                            onAction = onAction,
                            playFocusRequester = playFocusRequester,
                            actionsFocusRequester = actionsFocusRequester,
                            trailerFocusRequester = trailerFocusRequester,
                            likeFocusRequester = likeFocusRequester,
                            myListFocusRequester = myListFocusRequester,
                            recommendationsFocusRequester = recommendationsFocusRequester,
                            sharedElementScope = sharedElementScope,
                            overviewFocusRequester = overviewFocusRequester,
                            scrollState = scrollState,
                            viewportCoordinates = { verticalBringIntoViewSpec.viewportCoordinates },
                            modifier = Modifier
                                .onGloballyPositioned { verticalBringIntoViewSpec.summaryCoordinates = it }
                                .padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
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
private fun SummarySection(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    sharedElementScope: StreamCoreSharedElementScope?,
    overviewFocusRequester: FocusRequester,
    scrollState: ScrollState,
    viewportCoordinates: () -> LayoutCoordinates?,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier.weight(0.48f),
        ) {
            DetailsHero(
                content = content,
                sharedElementScope = sharedElementScope,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.streamCoreSharedBounds(
                    sharedElementScope = sharedElementScope,
                    key = StreamCoreSharedKey.title(contentId = content.id, row = content.row),
                    clipShape = RectangleShape,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                ),
            )
            Text(
                text = content.heroMetadata(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = StreamCoreDimens.Button.MinHeight)
                    .focusRequester(playFocusRequester)
                    .focusProperties {
                        up = FocusRequester.Cancel
                        left = FocusRequester.Cancel
                        right = if (state.isLibraryAvailable || content.trailers.isNotEmpty()) {
                            actionsFocusRequester
                        } else {
                            overviewFocusRequester
                        }
                        down = if (state.recommendations.isNotEmpty()) {
                            recommendationsFocusRequester
                        } else {
                            overviewFocusRequester
                        }
                    }
                    .testTag(DetailsTestTags.PlayButton),
            )
        }
        DetailsInformation(
            state = state,
            onAction = onAction,
            playFocusRequester = playFocusRequester,
            actionsFocusRequester = actionsFocusRequester,
            trailerFocusRequester = trailerFocusRequester,
            likeFocusRequester = likeFocusRequester,
            myListFocusRequester = myListFocusRequester,
            recommendationsFocusRequester = recommendationsFocusRequester,
            overviewFocusRequester = overviewFocusRequester,
            scrollState = scrollState,
            viewportCoordinates = viewportCoordinates,
            modifier = Modifier.weight(0.52f),
        )
    }
}

@Composable
private fun DetailsHero(
    content: ContentModel,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    val heroShape = MaterialTheme.shapes.extraLarge

    StreamCoreContentImage(
        imageUrl = content.backdrop ?: content.poster,
        contentDescription = content.title,
        fallbackText = content.fallbackText(),
        contentScale = ContentScale.Crop,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        fallbackTextStyle = MaterialTheme.typography.displayLarge,
        crossfade = true,
        modifier = modifier
            .testTag(DetailsTestTags.Hero)
            .streamCoreSharedBounds(
                sharedElementScope = sharedElementScope,
                key = StreamCoreSharedKey.artwork(
                    contentId = content.id,
                    row = content.row,
                ),
                clipShape = heroShape,
            ),
    )
}

@Composable
private fun DetailsInformation(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    overviewFocusRequester: FocusRequester,
    scrollState: ScrollState,
    viewportCoordinates: () -> LayoutCoordinates?,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier,
    ) {
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
        if (state.isLoading) {
            StreamCoreLoadingChip(text = "Updating")
        }
        DetailsReadingOverview(
            content = content,
            scrollState = scrollState,
            viewportCoordinates = viewportCoordinates,
            overviewFocusRequester = overviewFocusRequester,
            upFocusRequester = if (state.isLibraryAvailable || content.trailers.isNotEmpty()) {
                actionsFocusRequester
            } else {
                playFocusRequester
            },
            playFocusRequester = playFocusRequester,
            downFocusRequester = if (state.recommendations.isNotEmpty()) {
                recommendationsFocusRequester
            } else {
                FocusRequester.Cancel
            },
        )
    }
}

@Composable
private fun DetailsReadingOverview(
    content: ContentModel,
    scrollState: ScrollState,
    viewportCoordinates: () -> LayoutCoordinates?,
    overviewFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }
    var readingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()

    DetailsOverview(
        content = content,
        headingStyle = MaterialTheme.typography.titleSmall,
        genresStyle = MaterialTheme.typography.labelLarge,
        descriptionStyle = MaterialTheme.typography.bodyMedium,
        castHeadingStyle = MaterialTheme.typography.titleSmall,
        castStyle = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .border(
                width = StreamCoreDimens.Tv.Focus.BorderWidth,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.transparentContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(StreamCoreDimens.Spacing.Small)
            .onGloballyPositioned { readingCoordinates = it }
            .focusRequester(overviewFocusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusProperties {
                up = upFocusRequester
                down = downFocusRequester
                left = playFocusRequester
                right = FocusRequester.Cancel
            }
            .onPreviewKeyEvent { event ->
                if (event.key != Key.DirectionUp && event.key != Key.DirectionDown) {
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
                    minOf(readingStep, maxOf(0f, bounds.bottom - viewportHeight))
                } else {
                    -minOf(readingStep, maxOf(0f, -bounds.top))
                }
                val canScroll = if (distance > 1f) {
                    scrollState.canScrollForward
                } else if (distance < -1f) {
                    scrollState.canScrollBackward
                } else {
                    false
                }
                if (canScroll) {
                    // A user scroll cancels relocation before moving the same outer viewport.
                    // The composition scope cancels pending work when this content leaves.
                    coroutineScope.launch {
                        scrollState.scroll(MutatePriority.UserInput) {
                            scrollBy(distance)
                        }
                    }
                } else {
                    val target = if (event.key == Key.DirectionDown) downFocusRequester else upFocusRequester
                    if (target != FocusRequester.Cancel) {
                        target.requestFocus()
                    }
                }
                true
            }
            .focusable(),
    )
}

private const val ReadingScrollFraction = 0.6f

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
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
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
                .weight(1f)
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
                .weight(1f)
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
                    .weight(1f)
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
        !isAvailable -> StreamCoreControlDefaults.style().disabledContent
        selected == true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
            style = MaterialTheme.typography.titleLarge,
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
                    onClick = { onAction(DetailsAction.RecommendationSelected(content)) },
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
            titleStyle = MaterialTheme.typography.labelLarge,
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
