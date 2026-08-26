package com.pampoukidis.streamcoretv.feature.details.tv.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreDelayedEntrance
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

@Composable
fun TvDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val (
        backFocusRequester,
        refreshFocusRequester,
        playFocusRequester,
        likeFocusRequester,
        myListFocusRequester,
        firstRecommendationFocusRequester,
    ) = remember { FocusRequester.createRefs() }
    val contentId = state.content?.id

    LaunchedEffect(contentId) {
        if (contentId == null) {
            backFocusRequester.requestFocus()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = StreamCoreDimens.Tv.Screen.VerticalPadding),
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
            DetailsBody(
                state = state,
                onAction = onAction,
                backFocusRequester = backFocusRequester,
                playFocusRequester = playFocusRequester,
                likeFocusRequester = likeFocusRequester,
                myListFocusRequester = myListFocusRequester,
                firstRecommendationFocusRequester = firstRecommendationFocusRequester,
                sharedElementScope = sharedElementScope,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

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
        StreamCoreTvButton(
            text = "Refresh",
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

@Composable
private fun DetailsBody(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    backFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    firstRecommendationFocusRequester: FocusRequester,
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

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Tv.Screen.VerticalPadding),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(DetailsTestTags.Content),
            ) {
                item(contentType = "summary") {
                    SummarySection(
                        state = state,
                        onAction = onAction,
                        backFocusRequester = backFocusRequester,
                        playFocusRequester = playFocusRequester,
                        likeFocusRequester = likeFocusRequester,
                        myListFocusRequester = myListFocusRequester,
                        firstRecommendationFocusRequester = firstRecommendationFocusRequester,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.padding(
                            horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                        ),
                    )
                }
                item(contentType = "recommendations") {
                    StreamCoreDelayedEntrance(
                        visibleKey = "${content.id}:recommendations",
                        delayMillis = RecommendationsEntranceDelayMillis,
                    ) {
                        RecommendationsRow(
                            recommendations = state.recommendations,
                            onAction = onAction,
                            firstRecommendationFocusRequester = firstRecommendationFocusRequester,
                            actionsUpFocusRequester = if (state.isLibraryAvailable) {
                                likeFocusRequester
                            } else {
                                playFocusRequester
                            },
                        )
                    }
                }
            }
        }

        if (state.isLoading && content != null) {
            StreamCoreLoadingChip(
                text = "Updating",
                textStyle = MaterialTheme.typography.labelLarge,
                indicatorSize = StreamCoreDimens.Icon.Medium,
                contentPadding = PaddingValues(
                    horizontal = StreamCoreDimens.Spacing.Large,
                    vertical = StreamCoreDimens.Spacing.Small,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = StreamCoreDimens.Spacing.Medium,
                        end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                    ),
            )
        }
    }
}

@Composable
private fun SummarySection(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    backFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    firstRecommendationFocusRequester: FocusRequester,
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
                .weight(0.56f)
                .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
        )
        DetailsMetadata(
            state = state,
            onAction = onAction,
            backFocusRequester = backFocusRequester,
            playFocusRequester = playFocusRequester,
            likeFocusRequester = likeFocusRequester,
            myListFocusRequester = myListFocusRequester,
            firstRecommendationFocusRequester = firstRecommendationFocusRequester,
            sharedElementScope = sharedElementScope,
            modifier = Modifier.weight(0.44f),
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
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    firstRecommendationFocusRequester: FocusRequester,
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
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier,
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.streamCoreSharedBounds(
                sharedElementScope = sharedElementScope,
                key = StreamCoreSharedKey.title(
                    contentId = content.id,
                    row = content.row,
                ),
                clipShape = RectangleShape,
                zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
            ),
        )
        StreamCoreDelayedEntrance(
            visibleKey = content.id,
            delayMillis = MetadataEntranceDelayMillis,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${releaseYear(content.releaseDate)} · ${content.pgRatingName} · ${content.rating}/10",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = genreText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DetailsActions(
                    contentId = content.id,
                    hasResumableProgress = state.hasResumableProgress,
                    isLibraryAvailable = state.isLibraryAvailable,
                    isLiked = state.isLiked,
                    isInMyList = state.isInMyList,
                    isLikeMutationPending = state.isLikeMutationPending,
                    isMyListMutationPending = state.isMyListMutationPending,
                    hasRecommendations = state.recommendations.isNotEmpty(),
                    backFocusRequester = backFocusRequester,
                    playFocusRequester = playFocusRequester,
                    likeFocusRequester = likeFocusRequester,
                    myListFocusRequester = myListFocusRequester,
                    firstRecommendationFocusRequester = firstRecommendationFocusRequester,
                    onPlayClick = { onAction(DetailsAction.PlaySelected) },
                    onLikeClick = { onAction(DetailsAction.LikeToggled) },
                    onMyListClick = { onAction(DetailsAction.MyListToggled) },
                )
                Text(
                    text = content.description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                if (castText.isNotBlank()) {
                    Text(
                        text = castText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsActions(
    contentId: String,
    hasResumableProgress: Boolean,
    isLibraryAvailable: Boolean,
    isLiked: Boolean,
    isInMyList: Boolean,
    isLikeMutationPending: Boolean,
    isMyListMutationPending: Boolean,
    hasRecommendations: Boolean,
    backFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    likeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    firstRecommendationFocusRequester: FocusRequester,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMyListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(contentId) {
        playFocusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = TvActionsMaxWidth)
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
                .fillMaxWidth()
                .focusRequester(playFocusRequester)
                .focusProperties {
                    up = backFocusRequester
                    down = when {
                        isLibraryAvailable -> likeFocusRequester
                        hasRecommendations -> firstRecommendationFocusRequester
                        else -> FocusRequester.Cancel
                    }
                }
                .testTag(DetailsTestTags.PlayButton),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
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
                        up = playFocusRequester
                        right = myListFocusRequester
                        down = if (hasRecommendations) {
                            firstRecommendationFocusRequester
                        } else {
                            FocusRequester.Cancel
                        }
                    }
                    .testTag(DetailsTestTags.LikeAction),
            ) {
                StreamCoreHeartIcon(filled = isLiked)
            }
            DetailsLibraryButton(
                label = stringResource(R.string.details_action_my_list),
                selectedStateDescription = stringResource(
                    if (isInMyList) {
                        R.string.details_my_list_selected
                    } else {
                        R.string.details_my_list_unselected
                    },
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
                        up = playFocusRequester
                        left = likeFocusRequester
                        down = if (hasRecommendations) {
                            firstRecommendationFocusRequester
                        } else {
                            FocusRequester.Cancel
                        }
                    }
                    .testTag(DetailsTestTags.MyListAction),
            ) {
                StreamCoreBookmarkIcon(filled = isInMyList)
            }
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
    firstRecommendationFocusRequester: FocusRequester,
    actionsUpFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tv.Focus.BorderPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            itemsIndexed(
                items = recommendations,
                key = { _, content -> content.id },
                contentType = { _, _ -> "recommendation" },
            ) { index, content ->
                RecommendationCard(
                    content = content,
                    onClick = {
                        onAction(DetailsAction.RecommendationSelected(content))
                    },
                    modifier = if (index == 0) {
                        Modifier
                            .focusRequester(firstRecommendationFocusRequester)
                            .focusProperties { up = actionsUpFocusRequester }
                    } else {
                        Modifier
                    },
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
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
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
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Spacing.Large,
                    vertical = StreamCoreDimens.Spacing.Medium,
                ),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${content.rating}/10 · ${content.pgRatingName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun releaseYear(epochMillis: Long): Int {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = epochMillis
    return calendar.get(Calendar.YEAR)
}

private const val MetadataEntranceDelayMillis = 90
private const val RecommendationsEntranceDelayMillis = 170
private val TvActionsMaxWidth = 420.dp

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
