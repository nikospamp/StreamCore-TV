package com.pampoukidis.streamcoretv.feature.details.tablet.details

import com.pampoukidis.streamcoretv.core.tracing.benchmarkReadiness

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButtonVariant
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTrailerIcon
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreDelayedEntrance
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.tablet.R
import java.util.Calendar
import java.util.TimeZone

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
            DetailsHeader(
                isLoading = state.isLoading,
                onAction = onAction,
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
private fun DetailsHeader(
    isLoading: Boolean,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        StreamCoreTextButton(
            text = "Back",
            onClick = { onAction(DetailsAction.BackSelected) },
            enabled = true,
            modifier = Modifier.testTag(DetailsTestTags.BackButton),
        )
        StreamCoreTextButton(
            text = "Refresh",
            onClick = { onAction(DetailsAction.Refresh) },
            enabled = !isLoading,
            modifier = Modifier.testTag(DetailsTestTags.RefreshButton),
        )
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
        DetailsMetadata(
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
private fun DetailsMetadata(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier,
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.displaySmall,
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
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${releaseYear(content.releaseDate)} · ${content.pgRatingName} · ${content.rating}/10",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = genreText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Text(
                    text = content.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (castText.isNotBlank()) {
                    Text(
                        text = castText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
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
                    .testTag(DetailsTestTags.MyListAction),
            ) {
                StreamCoreBookmarkIcon(filled = isInMyList)
            }
            if (isTrailerAvailable) {
                StreamCoreButton(
                    text = stringResource(R.string.details_action_trailer),
                    onClick = onTrailerClick,
                    enabled = true,
                    variant = StreamCoreButtonVariant.Secondary,
                    leadingIcon = { StreamCoreTrailerIcon() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(DetailsTestTags.TrailerAction),
                )
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
    val iconColor = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = ActionStateAnimationMillis),
        label = "detailsLibraryActionColor",
    )

    StreamCoreButton(
        text = label,
        onClick = onClick,
        enabled = isAvailable && !isLoading,
        loading = isLoading,
        variant = StreamCoreButtonVariant.Secondary,
        leadingIcon = {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides iconColor.value) {
                icon()
            }
        },
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
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = StreamCoreDimens.Elevation.Low,
        modifier = modifier
            .width(StreamCoreDimens.Tablet.Details.RecommendationCardWidth)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
            StreamCoreContentImage(
                imageUrl = content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                fallbackTextStyle = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                    .clip(MaterialTheme.shapes.large),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Spacing.Medium,
                    vertical = StreamCoreDimens.Spacing.Medium,
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

private const val MetadataEntranceDelayMillis = 80
private const val RecommendationsEntranceDelayMillis = 150
private const val ActionStateAnimationMillis = 180
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
