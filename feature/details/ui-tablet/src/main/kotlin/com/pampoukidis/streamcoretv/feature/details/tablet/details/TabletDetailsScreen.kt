package com.pampoukidis.streamcoretv.feature.details.tablet.details

import com.pampoukidis.streamcoretv.core.tracing.benchmarkReadiness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreDelayedEntrance
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsOverview
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRecommendationArtwork
import com.pampoukidis.streamcoretv.feature.details.common.touch.details.DetailsTouchActions
import com.pampoukidis.streamcoretv.feature.details.common.touch.details.DetailsTouchTopControls
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.tablet.R

@Composable
fun TabletDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root)
            .benchmarkReadiness("details-tablet", !state.isLoading && state.content != null),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = StreamCoreDimens.Tablet.Screen.VerticalPadding),
        ) {
            DetailsTouchTopControls(
                isLoading = state.isLoading,
                onBack = { onAction(DetailsAction.BackSelected) },
                onRefresh = { onAction(DetailsAction.Refresh) },
                modifier = Modifier.padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
            )
            DetailsBody(
                state = state,
                onAction = onAction,
                sharedElementScope = sharedElementScope,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailsBody(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(DetailsTestTags.Content),
            ) {
                item(contentType = "summary") {
                    SummarySection(
                        state = state,
                        onAction = onAction,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.padding(
                            horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
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
                        )
                    }
                }
            }
        }

        if (state.isLoading && content != null) {
            StreamCoreLoadingChip(
                text = "Updating",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = StreamCoreDimens.Spacing.Medium,
                        end = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                    ),
            )
        }
    }
}

@Composable
private fun SummarySection(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
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
                .weight(0.44f)
                .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
        )
        DetailsInformation(
            state = state,
            onAction = onAction,
            sharedElementScope = sharedElementScope,
            modifier = Modifier.weight(0.56f),
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
private fun DetailsTitle(
    content: ContentModel,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
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
        Text(
            text = content.heroMetadata(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailsInformation(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier,
    ) {
        DetailsTitle(
            content = content,
            sharedElementScope = sharedElementScope,
        )
        DetailsActions(
            hasResumableProgress = state.hasResumableProgress,
            isTrailerAvailable = content.trailers.isNotEmpty(),
            isLibraryAvailable = state.isLibraryAvailable,
            isLiked = state.isLiked,
            isInMyList = state.isInMyList,
            isLikeMutationPending = state.isLikeMutationPending,
            isMyListMutationPending = state.isMyListMutationPending,
            onPlayClick = { onAction(DetailsAction.PlaySelected) },
            onTrailerClick = { onAction(DetailsAction.TrailerSelected) },
            onLikeClick = { onAction(DetailsAction.LikeToggled) },
            onMyListClick = { onAction(DetailsAction.MyListToggled) },
        )
        StreamCoreDelayedEntrance(
            visibleKey = content.id,
            delayMillis = MetadataEntranceDelayMillis,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DetailsOverview(content = content)
        }
    }
}

@Composable
private fun DetailsActions(
    hasResumableProgress: Boolean,
    isTrailerAvailable: Boolean,
    isLibraryAvailable: Boolean,
    isLiked: Boolean,
    isInMyList: Boolean,
    isLikeMutationPending: Boolean,
    isMyListMutationPending: Boolean,
    onPlayClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMyListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = DetailsActionsMaxWidth),
    ) {
        StreamCoreButton(
            text = stringResource(
                if (hasResumableProgress) R.string.details_action_resume else R.string.details_action_play,
            ),
            onClick = onPlayClick,
            enabled = true,
            leadingIcon = { StreamCorePlayIcon() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DetailsTestTags.PlayButton),
        )
        DetailsTouchActions(
            isLibraryAvailable = isLibraryAvailable,
            isLiked = isLiked,
            isInMyList = isInMyList,
            isLikeMutationPending = isLikeMutationPending,
            isMyListMutationPending = isMyListMutationPending,
            isTrailerAvailable = isTrailerAvailable,
            onLikeClick = onLikeClick,
            onMyListClick = onMyListClick,
            onTrailerClick = onTrailerClick,
        )
    }
}

@Composable
private fun RecommendationsRow(
    recommendations: List<ContentModel>,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        ) {
            items(
                items = recommendations,
                key = { it.id },
                contentType = { "recommendation" },
            ) { content ->
                RecommendationCard(
                    content = content,
                    onClick = {
                        onAction(DetailsAction.RecommendationSelected(content))
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
    DetailsRecommendationArtwork(
        content = content,
        modifier = modifier
            .width(StreamCoreDimens.Tablet.Details.RecommendationCardWidth)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    )
}

private const val MetadataEntranceDelayMillis = 80
private const val RecommendationsEntranceDelayMillis = 150
private val DetailsActionsMaxWidth = StreamCoreDimens.Tablet.Details.ActionsMaxWidth

@PreviewTablet
@Composable
private fun TabletDetailsScreenDarkPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
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

@PreviewTablet
@Composable
private fun TabletDetailsScreenResumeSelectedLightPreview() {
    StreamCoreTheme(darkTheme = false) {
        TabletDetailsScreen(
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

@PreviewTablet
@Composable
private fun TabletDetailsScreenRetainedLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = true,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
                isLikeMutationPending = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenUnavailableActionsPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
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

@PreviewTablet
@Composable
private fun TabletDetailsScreenTrailerPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content.copy(
                    trailers = listOf(
                        TrailerModel(
                            id = "official-trailer",
                            title = "Official trailer",
                            url = "https://example.test/trailer",
                        ),
                    ),
                ),
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenLongTitlePreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content.copy(
                    title = "The Extraordinary Adventures Beyond the Edge of the Known Universe",
                ),
                recommendations = DetailsPreviewData.recommendations,
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}
