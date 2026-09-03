package com.pampoukidis.streamcoretv.feature.details.web.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtwork
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebContentCard
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.web.testing.WebDetailsFixtures
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.details.ui_web.generated.resources.Res
import streamcoretv.feature.details.ui_web.generated.resources.web_details_action_unavailable
import streamcoretv.feature.details.ui_web.generated.resources.web_details_action_updating
import streamcoretv.feature.details.ui_web.generated.resources.web_details_back
import streamcoretv.feature.details.ui_web.generated.resources.web_details_cast
import streamcoretv.feature.details.ui_web.generated.resources.web_details_error_message
import streamcoretv.feature.details.ui_web.generated.resources.web_details_error_title
import streamcoretv.feature.details.ui_web.generated.resources.web_details_genres
import streamcoretv.feature.details.ui_web.generated.resources.web_details_in_my_list
import streamcoretv.feature.details.ui_web.generated.resources.web_details_like
import streamcoretv.feature.details.ui_web.generated.resources.web_details_liked
import streamcoretv.feature.details.ui_web.generated.resources.web_details_loading
import streamcoretv.feature.details.ui_web.generated.resources.web_details_my_list
import streamcoretv.feature.details.ui_web.generated.resources.web_details_no_recommendations
import streamcoretv.feature.details.ui_web.generated.resources.web_details_overview
import streamcoretv.feature.details.ui_web.generated.resources.web_details_play
import streamcoretv.feature.details.ui_web.generated.resources.web_details_recommendations
import streamcoretv.feature.details.ui_web.generated.resources.web_details_refresh
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
    val refreshFocusRequester = remember { FocusRequester() }
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
                WebDetailsRefreshItem -> refreshFocusRequester
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
        WebDetailsHeader(
            state = state,
            backFocusRequester = backFocusRequester,
            refreshFocusRequester = refreshFocusRequester,
            retryFocusRequester = retryFocusRequester,
            playFocusRequester = playFocusRequester,
            onAction = onAction,
        )

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
            )
        }
    }
}

@Composable
private fun WebDetailsHeader(
    state: DetailsUiState,
    backFocusRequester: FocusRequester,
    refreshFocusRequester: FocusRequester,
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        StreamCoreWebButton(
            text = stringResource(Res.string.web_details_back),
            onClick = { onAction(DetailsAction.BackSelected) },
            variant = StreamCoreWebButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(backFocusRequester)
                .focusProperties {
                    right = if (state.isLoading) FocusRequester.Cancel else refreshFocusRequester
                    down = downRequester
                }
                .testTag(DetailsTestTags.BackButton),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isLoading && state.content != null) {
                Text(
                    text = stringResource(Res.string.web_details_updating),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            StreamCoreWebButton(
                text = stringResource(Res.string.web_details_refresh),
                onClick = { onAction(DetailsAction.Refresh) },
                enabled = !state.isLoading,
                loading = state.isLoading,
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier
                    .focusRequester(refreshFocusRequester)
                    .focusProperties {
                        left = backFocusRequester
                        down = downRequester
                    }
                    .testTag(DetailsTestTags.RefreshButton),
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
        Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge)) {
            Box(
                modifier = Modifier
                    .weight(0.52f)
                    .aspectRatio(HeroAspectRatio)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.weight(0.48f),
            ) {
                repeat(LoadingLineCount) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 0) 0.78f else 1f)
                            .height(if (index == 0) LoadingTitleHeight else LoadingLineHeight)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }
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
        modifier = Modifier.testTag(DetailsTestTags.Error),
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
                    .focusProperties { up = backFocusRequester }
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
) {
    val content = requireNotNull(state.content)
    val recommendationRequester = recommendationFocusRequesters.firstOrNull()
        ?: FocusRequester.Cancel
    val metadata = remember(content) { content.heroMetadata() }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Content),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StreamCoreWebArtwork(
                imageUrl = content.backdrop ?: content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                requestWidthPx = HeroRequestWidthPx,
                requestHeightPx = HeroRequestHeightPx,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .weight(0.52f)
                    .aspectRatio(HeroAspectRatio)
                    .testTag(DetailsTestTags.Hero),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.weight(0.48f),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WebDetailsMetadata(content)
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
    val firstLibraryRequester = when {
        likeEnabled -> likeFocusRequester
        myListEnabled -> myListFocusRequester
        else -> FocusRequester.Cancel
    }
    val playRightRequester = when {
        hasTrailer -> trailerFocusRequester
        else -> firstLibraryRequester
    }
    val myListLeftRequester = when {
        likeEnabled -> likeFocusRequester
        hasTrailer -> trailerFocusRequester
        else -> playFocusRequester
    }
    val likeRightRequester = when {
        myListEnabled -> myListFocusRequester
        else -> FocusRequester.Cancel
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        StreamCoreWebButton(
            text = stringResource(
                if (state.hasResumableProgress) {
                    Res.string.web_details_resume
                } else {
                    Res.string.web_details_play
                },
            ),
            onClick = { onAction(DetailsAction.PlaySelected) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(playFocusRequester)
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = playRightRequester
                    up = backFocusRequester
                    down = recommendationsFocusRequester
                }
                .testTag(DetailsTestTags.PlayButton),
        )
        if (hasTrailer) {
            StreamCoreWebButton(
                text = stringResource(Res.string.web_details_trailer),
                onClick = { onAction(DetailsAction.TrailerSelected) },
                variant = StreamCoreWebButtonVariant.Secondary,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(trailerFocusRequester)
                    .focusProperties {
                        left = playFocusRequester
                        right = firstLibraryRequester
                        up = backFocusRequester
                        down = recommendationsFocusRequester
                    }
                    .testTag(DetailsTestTags.TrailerAction),
            )
        }
        WebDetailsLibraryAction(
            text = stringResource(
                if (state.isLiked) Res.string.web_details_liked else Res.string.web_details_like,
            ),
            selected = state.isLiked,
            available = state.isLibraryAvailable,
            loading = state.isLikeMutationPending,
            focusRequester = likeFocusRequester,
            leftFocusRequester = if (hasTrailer) trailerFocusRequester else playFocusRequester,
            rightFocusRequester = likeRightRequester,
            upFocusRequester = backFocusRequester,
            downFocusRequester = recommendationsFocusRequester,
            testTag = DetailsTestTags.LikeAction,
            onClick = { onAction(DetailsAction.LikeToggled) },
        )
        WebDetailsLibraryAction(
            text = stringResource(
                if (state.isInMyList) {
                    Res.string.web_details_in_my_list
                } else {
                    Res.string.web_details_my_list
                },
            ),
            selected = state.isInMyList,
            available = state.isLibraryAvailable,
            loading = state.isMyListMutationPending,
            focusRequester = myListFocusRequester,
            leftFocusRequester = myListLeftRequester,
            rightFocusRequester = FocusRequester.Cancel,
            upFocusRequester = backFocusRequester,
            downFocusRequester = recommendationsFocusRequester,
            testTag = DetailsTestTags.MyListAction,
            onClick = { onAction(DetailsAction.MyListToggled) },
        )
    }
}

@Composable
private fun RowScope.WebDetailsLibraryAction(
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
) {
    val stateText = when {
        !available -> stringResource(Res.string.web_details_action_unavailable)
        loading -> stringResource(Res.string.web_details_action_updating)
        else -> text
    }
    StreamCoreWebButton(
        text = text,
        onClick = onClick,
        enabled = available,
        loading = loading,
        variant = StreamCoreWebButtonVariant.Secondary,
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = available && !loading
                left = leftFocusRequester
                right = rightFocusRequester
                up = upFocusRequester
                down = downFocusRequester
            }
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                this.selected = selected
                stateDescription = stateText
                if (!available || loading) {
                    disabled()
                }
            }
            .testTag(testTag),
    )
}

@Composable
private fun WebDetailsMetadata(content: ContentModel) {
    val genresLabel = stringResource(Res.string.web_details_genres)
    val castLabel = stringResource(Res.string.web_details_cast)
    val genresText = remember(genresLabel, content.genres) {
        genresLabel + ": " + content.genres.joinToString { genre -> genre.name }
    }
    val castText = remember(castLabel, content.cast) {
        castLabel + ": " + content.cast.joinToString { castMember -> castMember.name }
    }
    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
        Text(
            text = stringResource(Res.string.web_details_overview),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = content.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(DetailsTestTags.Overview),
        )
        if (content.genres.isNotEmpty()) {
            Text(
                text = genresText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (content.cast.isNotEmpty()) {
            Text(
                text = castText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = stringResource(Res.string.web_details_recommendations),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
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
                contentPadding = PaddingValues(StreamCoreDimens.Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.fillMaxWidth(),
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
    val scrimColor = MaterialTheme.colorScheme.scrim
    val scrim = remember(scrimColor) {
        Brush.verticalGradient(
            0.45f to Color.Transparent,
            1f to scrimColor.copy(alpha = 0.92f),
        )
    }
    StreamCoreWebContentCard(
        onClick = onClick,
        aspectRatio = RecommendationAspectRatio,
        modifier = modifier.width(RecommendationCardWidth),
    ) {
        StreamCoreWebArtwork(
            imageUrl = content.backdrop ?: content.poster,
            contentDescription = content.title,
            fallbackText = content.fallbackText(),
            requestWidthPx = RecommendationRequestWidthPx,
            requestHeightPx = RecommendationRequestHeightPx,
            modifier = Modifier.fillMaxSize(),
            overlay = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrim),
                )
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(StreamCoreDimens.Spacing.Medium),
                )
            },
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

private const val HeroRequestWidthPx = 1280
private const val HeroRequestHeightPx = 720
private const val RecommendationRequestWidthPx = 480
private const val RecommendationRequestHeightPx = 270
private const val LoadingLineCount = 5
private const val FocusRequestAttempts = 3
private const val RecommendationContentType = "details-recommendation"
private val HeroAspectRatio = 16f / 9f
private val RecommendationAspectRatio = 16f / 9f
private val RecommendationCardWidth = 280.dp
private val LoadingTitleHeight = 44.dp
private val LoadingLineHeight = 24.dp

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
            modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge),
        )
    }
}
