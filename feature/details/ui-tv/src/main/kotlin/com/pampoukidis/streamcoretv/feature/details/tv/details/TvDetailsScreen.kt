package com.pampoukidis.streamcoretv.feature.details.tv.details

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component4
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component5
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component6
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component7
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTrailerIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.tv.R
import java.util.Calendar
import java.util.TimeZone

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
        backFocusRequester,
        refreshFocusRequester,
        playFocusRequester,
        likeFocusRequester,
        myListFocusRequester,
        trailerFocusRequester,
        recommendationsFocusRequester,
        actionsFocusRequester,
    ) = remember { FocusRequester.createRefs() }
    val contentId = state.content?.id
    val currentReturnFocusKey by rememberUpdatedState(returnFocusKey)

    LifecycleResumeEffect(contentId) {
        if (currentReturnFocusKey == null) {
            if (contentId == null) {
                backFocusRequester.requestFocus()
            } else {
                playFocusRequester.requestFocus()
            }
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
                .padding(vertical = StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            DetailsHeader(
                isLoading = state.isLoading,
                hasContent = state.content != null,
                backFocusRequester = backFocusRequester,
                refreshFocusRequester = refreshFocusRequester,
                playFocusRequester = playFocusRequester,
                onAction = onAction,
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                ),
            )
            // TV's default pivot scrolls even visible controls, cropping the hero as it
            // leaves the shared-transition overlay. Only scroll to reveal off-screen content.
            CompositionLocalProvider(LocalBringIntoViewSpec provides DetailsBringIntoViewSpec) {
                DetailsBody(
                    state = state,
                    onAction = onAction,
                    backFocusRequester = backFocusRequester,
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

@Composable
private fun DetailsHeader(
    isLoading: Boolean,
    hasContent: Boolean,
    backFocusRequester: FocusRequester,
    refreshFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        StreamCoreTvButton(
            text = "Back",
            variant = StreamCoreTvButtonVariant.Tertiary,
            onClick = { onAction(DetailsAction.BackSelected) },
            enabled = true,
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .focusProperties {
                    right = refreshFocusRequester
                    down = if (hasContent) playFocusRequester else FocusRequester.Default
                }
                .testTag(DetailsTestTags.BackButton),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading && hasContent) {
                StreamCoreLoadingChip(text = "Updating")
            }
            StreamCoreTvButton(
                text = "Refresh",
                variant = StreamCoreTvButtonVariant.Tertiary,
                onClick = { onAction(DetailsAction.Refresh) },
                enabled = !isLoading,
                modifier = Modifier
                    .focusRequester(refreshFocusRequester)
                    .focusProperties {
                        left = backFocusRequester
                        down = if (hasContent) playFocusRequester else FocusRequester.Default
                    }
                    .testTag(DetailsTestTags.RefreshButton),
            )
        }
    }
}

@Composable
private fun DetailsBody(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    backFocusRequester: FocusRequester,
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && content == null -> CircularProgressIndicator()
            content == null -> Text(
                text = "Unable to load details.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // There are only two vertical sections. Keep both composed so a long summary
            // cannot detach the recommendations' focus target. The horizontal row stays lazy.
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(DetailsTestTags.Content)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = StreamCoreDimens.Spacing.Large),
            ) {
                SummarySection(
                    state = state,
                    onAction = onAction,
                    backFocusRequester = backFocusRequester,
                    playFocusRequester = playFocusRequester,
                    actionsFocusRequester = actionsFocusRequester,
                    trailerFocusRequester = trailerFocusRequester,
                    likeFocusRequester = likeFocusRequester,
                    myListFocusRequester = myListFocusRequester,
                    recommendationsFocusRequester = recommendationsFocusRequester,
                    sharedElementScope = sharedElementScope,
                    modifier = Modifier.padding(
                        horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                    ),
                )
                RecommendationsRow(
                    recommendations = state.recommendations,
                    onAction = onAction,
                    recommendationsFocusRequester = recommendationsFocusRequester,
                    actionsUpFocusRequester = actionsFocusRequester,
                    fallbackFocusRequester = playFocusRequester,
                    returnFocusKey = returnFocusKey,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                )
            }
        }

    }
}

@Composable
private fun SummarySection(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    backFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier.fillMaxWidth(),
    ) {
        DetailsHero(
            content = content,
            sharedElementScope = sharedElementScope,
            modifier = Modifier
                .weight(0.48f)
                .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
        )
        DetailsMetadata(
            state = state,
            onAction = onAction,
            backFocusRequester = backFocusRequester,
            playFocusRequester = playFocusRequester,
            actionsFocusRequester = actionsFocusRequester,
            trailerFocusRequester = trailerFocusRequester,
            likeFocusRequester = likeFocusRequester,
            myListFocusRequester = myListFocusRequester,
            recommendationsFocusRequester = recommendationsFocusRequester,
            sharedElementScope = sharedElementScope,
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
private fun DetailsMetadata(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    backFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { it.name }
    }
    val castText = remember(content.cast) {
        content.cast.joinToString(separator = " · ") { cast ->
            cast.characterName?.let { "${cast.name} as $it" } ?: cast.name
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier,
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.streamCoreSharedBounds(
                sharedElementScope = sharedElementScope,
                key = StreamCoreSharedKey.title(contentId = content.id, row = content.row),
                clipShape = RectangleShape,
                zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
            ),
        )
        Text(
            text = "${releaseYear(content.releaseDate)} · ${content.pgRatingName} · ${content.rating}/10",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (genreText.isNotBlank()) {
            Text(
                text = genreText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DetailsActions(
            contentId = content.id,
            hasResumableProgress = state.hasResumableProgress,
            hasTrailer = content.trailers.isNotEmpty(),
            isLibraryAvailable = state.isLibraryAvailable,
            isLiked = state.isLiked,
            isInMyList = state.isInMyList,
            isLikeMutationPending = state.isLikeMutationPending,
            isMyListMutationPending = state.isMyListMutationPending,
            hasRecommendations = state.recommendations.isNotEmpty(),
            backFocusRequester = backFocusRequester,
            playFocusRequester = playFocusRequester,
            actionsFocusRequester = actionsFocusRequester,
            trailerFocusRequester = trailerFocusRequester,
            likeFocusRequester = likeFocusRequester,
            myListFocusRequester = myListFocusRequester,
            recommendationsFocusRequester = recommendationsFocusRequester,
            onPlayClick = { onAction(DetailsAction.PlaySelected) },
            onTrailerClick = { onAction(DetailsAction.TrailerSelected) },
            onLikeClick = { onAction(DetailsAction.LikeToggled) },
            onMyListClick = { onAction(DetailsAction.MyListToggled) },
        )
        Text(
            text = content.description,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(DetailsTestTags.Overview),
        )
        if (castText.isNotBlank()) {
            Text(
                text = castText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailsActions(
    contentId: String,
    hasResumableProgress: Boolean,
    hasTrailer: Boolean,
    isLibraryAvailable: Boolean,
    isLiked: Boolean,
    isInMyList: Boolean,
    isLikeMutationPending: Boolean,
    isMyListMutationPending: Boolean,
    hasRecommendations: Boolean,
    backFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    actionsFocusRequester: FocusRequester,
    trailerFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    recommendationsFocusRequester: FocusRequester,
    onPlayClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMyListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(contentId) {
        playFocusRequester.requestFocus()
    }
    val downFocusRequester = if (hasRecommendations) {
        recommendationsFocusRequester
    } else {
        FocusRequester.Cancel
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(actionsFocusRequester)
            .focusRestorer(fallback = playFocusRequester)
            .focusGroup(),
    ) {
        StreamCoreTvButton(
            text = stringResource(
                if (hasResumableProgress) R.string.details_action_resume else R.string.details_action_play,
            ),
            onClick = onPlayClick,
            enabled = true,
            variant = StreamCoreTvButtonVariant.Primary,
            leadingIcon = { StreamCorePlayIcon() },
            modifier = Modifier
                .weight(1f)
                .focusRequester(playFocusRequester)
                .focusProperties {
                    up = backFocusRequester
                    left = FocusRequester.Cancel
                    right = when {
                        hasTrailer -> trailerFocusRequester
                        isLibraryAvailable -> likeFocusRequester
                        else -> FocusRequester.Cancel
                    }
                    down = downFocusRequester
                }
                .testTag(DetailsTestTags.PlayButton),
        )
        if (hasTrailer) {
            StreamCoreTvButton(
                text = stringResource(R.string.details_action_trailer),
                onClick = onTrailerClick,
                enabled = true,
                variant = StreamCoreTvButtonVariant.Secondary,
                leadingIcon = { StreamCoreTrailerIcon() },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(trailerFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                        left = playFocusRequester
                        right = if (isLibraryAvailable) likeFocusRequester else FocusRequester.Cancel
                        down = downFocusRequester
                    }
                    .testTag(DetailsTestTags.TrailerAction),
            )
        }
        DetailsLibraryButton(
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
                    up = backFocusRequester
                    left = if (hasTrailer) trailerFocusRequester else playFocusRequester
                    right = myListFocusRequester
                    down = downFocusRequester
                }
                .testTag(DetailsTestTags.LikeAction),
        ) {
            StreamCoreHeartIcon(filled = isLiked)
        }
        DetailsLibraryButton(
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
                    up = backFocusRequester
                    left = likeFocusRequester
                    right = FocusRequester.Cancel
                    down = downFocusRequester
                }
                .testTag(DetailsTestTags.MyListAction),
        ) {
            StreamCoreBookmarkIcon(filled = isInMyList)
        }
    }
}

@Composable
private fun DetailsLibraryButton(
    label: String,
    selectedStateDescription: String,
    selected: Boolean,
    isAvailable: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val stateDescription = when {
        !isAvailable -> stringResource(R.string.details_action_not_available)
        isLoading -> stringResource(R.string.details_action_updating)
        else -> selectedStateDescription
    }

    StreamCoreTvButton(
        text = label,
        onClick = onClick,
        enabled = isAvailable && !isLoading,
        loading = isLoading,
        variant = StreamCoreTvButtonVariant.Secondary,
        selected = selected,
        leadingIcon = icon,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label
            this.stateDescription = stateDescription
            this.selected = selected
            role = Role.Checkbox
            if (!isAvailable || isLoading) {
                disabled()
            }
        },
    )
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
            style = MaterialTheme.typography.headlineSmall,
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

    Surface(
        shape = MaterialTheme.shapes.large,
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
            .clickable(onClick = onClick)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        Column {
            StreamCoreContentImage(
                imageUrl = content.backdrop ?: content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fallbackTextStyle = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio)
                    .clip(MaterialTheme.shapes.large),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Spacing.Large,
                    vertical = StreamCoreDimens.Spacing.Small,
                ),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${content.rating}/10 · ${content.pgRatingName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

private fun releaseYear(epochMillis: Long): Int {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = epochMillis
    return calendar.get(Calendar.YEAR)
}

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
                ),
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}
