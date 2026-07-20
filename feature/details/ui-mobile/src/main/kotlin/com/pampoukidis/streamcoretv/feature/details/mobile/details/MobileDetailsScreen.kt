package com.pampoukidis.streamcoretv.feature.details.mobile.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreRefreshIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreDelayedEntrance
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreArtworkSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreTitleSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags

@Composable
fun MobileDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root),
    ) {
        val content = state.content

        when {
            content != null -> DetailsContent(
                content = content,
                recommendations = state.recommendations,
                isLoading = state.isLoading,
                onAction = onAction,
                sharedElementScope = sharedElementScope,
            )

            state.isLoading -> DetailsLoadingState(
                onBack = { onAction(DetailsAction.BackSelected) },
            )

            else -> DetailsUnavailableState(onAction = onAction)
        }
    }
}

@Composable
private fun DetailsContent(
    content: ContentModel,
    recommendations: List<ContentModel>,
    isLoading: Boolean,
    onAction: (DetailsAction) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Content),
    ) {
        item(
            key = "${content.id}:hero",
            contentType = "hero",
        ) {
            DetailsHero(
                content = content,
                isLoading = isLoading,
                onBack = { onAction(DetailsAction.BackSelected) },
                onRefresh = { onAction(DetailsAction.Refresh) },
                sharedElementScope = sharedElementScope,
            )
        }
        item(
            key = "${content.id}:overview",
            contentType = "overview",
        ) {
            StreamCoreDelayedEntrance(
                visibleKey = content.id,
                delayMillis = OverviewEntranceDelayMillis,
            ) {
                DetailsOverview(
                    content = content,
                    modifier = Modifier.padding(
                        horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                    ),
                )
            }
        }
        if (recommendations.isNotEmpty()) {
            item(
                key = "${content.id}:recommendations",
                contentType = "recommendations",
            ) {
                StreamCoreDelayedEntrance(
                    visibleKey = "${content.id}:recommendations",
                    delayMillis = RecommendationsEntranceDelayMillis,
                ) {
                    RecommendationsRow(
                        recommendations = recommendations,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsHero(
    content: ContentModel,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    val heroShape = MaterialTheme.shapes.extraLarge

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio)
            .streamCoreSharedBounds(
                sharedElementScope = sharedElementScope,
                key = streamCoreArtworkSharedKey(
                    contentId = content.id,
                    row = content.row,
                ),
                clipShape = heroShape,
            )
            .testTag(DetailsTestTags.Hero),
    ) {
        StreamCoreContentImage(
            imageUrl = content.backdrop ?: content.poster,
            contentDescription = content.title,
            fallbackText = content.fallbackText(),
            contentScale = ContentScale.Crop,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fallbackTextStyle = MaterialTheme.typography.displayLarge,
            modifier = Modifier.fillMaxSize(),
        )
        HeroScrim()
        DetailsTopControls(
            isLoading = isLoading,
            onBack = onBack,
            onRefresh = onRefresh,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(
                    horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                    vertical = StreamCoreDimens.Spacing.Small,
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                    vertical = StreamCoreDimens.Spacing.Large,
                ),
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onArtwork,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.streamCoreSharedBounds(
                    sharedElementScope = sharedElementScope,
                    key = streamCoreTitleSharedKey(
                        contentId = content.id,
                        row = content.row,
                    ),
                    clipShape = RectangleShape,
                ),
            )
            Text(
                text = content.heroMetadata(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = HeroSupportingContentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroScrim() {
    val scrim = MaterialTheme.colorScheme.scrim

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to scrim.copy(alpha = HeroTopScrimAlpha),
                        HeroScrimClearStop to scrim.copy(alpha = 0f),
                        1f to scrim.copy(alpha = HeroBottomScrimAlpha),
                    ),
                ),
            ),
    )
}

@Composable
private fun DetailsTopControls(
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        StreamCoreArtworkIconButton(
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier.testTag(DetailsTestTags.BackButton),
        ) {
            StreamCoreBackIcon()
        }
        if (isLoading) {
            StreamCoreLoadingChip(text = "Updating")
        } else {
            StreamCoreArtworkIconButton(
                contentDescription = "Refresh",
                onClick = onRefresh,
                modifier = Modifier.testTag(DetailsTestTags.RefreshButton),
            ) {
                StreamCoreRefreshIcon()
            }
        }
    }
}

@Composable
private fun DetailsOverview(
    content: ContentModel,
    modifier: Modifier = Modifier,
) {
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { genre -> genre.name }
    }
    val castText = remember(content.cast) {
        content.cast.joinToString(separator = " · ") { cast ->
            cast.characterName?.let { characterName ->
                "${cast.name} as $characterName"
            } ?: cast.name
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Overview),
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (genreText.isNotBlank()) {
            Text(
                text = genreText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = content.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (castText.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
                Text(
                    text = "Cast",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = castText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecommendationsRow(
    recommendations: List<ContentModel>,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        ) {
            items(
                items = recommendations,
                key = { recommendation -> recommendation.id },
                contentType = { "recommendation" },
            ) { recommendation ->
                RecommendationCard(
                    content = recommendation,
                    onClick = {
                        onAction(DetailsAction.RecommendationSelected(recommendation))
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
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier
            .width(StreamCoreDimens.Mobile.Details.RecommendationCardWidth)
            .clickable(onClick = onClick)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                .clip(MaterialTheme.shapes.medium),
        ) {
            StreamCoreContentImage(
                imageUrl = content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                fallbackTextStyle = MaterialTheme.typography.displayMedium,
                modifier = Modifier.fillMaxSize(),
            )
            RecommendationScrim()
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(StreamCoreDimens.Artwork.ContentPadding),
            )
        }
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecommendationScrim() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = RecommendationScrimAlpha),
                    ),
                ),
            ),
    )
}

@Composable
private fun DetailsLoadingState(
    onBack: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Loading),
    ) {
        item(contentType = "loading-hero") {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
            ) {
                DetailsTopControls(
                    isLoading = true,
                    onBack = onBack,
                    onRefresh = {},
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(
                            horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                            vertical = StreamCoreDimens.Spacing.Small,
                        ),
                )
            }
        }
        item(contentType = "loading-overview") {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                ),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(LoadingTitleWidthFraction)
                        .height(LoadingTitleHeight),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(LoadingMetadataWidthFraction)
                        .height(LoadingTextHeight),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LoadingBodyHeight),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
private fun DetailsUnavailableState(
    onAction: (DetailsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Mobile.Screen.VerticalPadding,
            )
            .testTag(DetailsTestTags.Error),
    ) {
        StreamCoreTextButton(
            text = "Back",
            onClick = { onAction(DetailsAction.BackSelected) },
            enabled = true,
            modifier = Modifier.testTag(DetailsTestTags.BackButton),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Details unavailable",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "We could not load this title. Check your connection and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StreamCoreButton(
                    text = "Try again",
                    onClick = { onAction(DetailsAction.Refresh) },
                    enabled = true,
                    modifier = Modifier.testTag(DetailsTestTags.RetryButton),
                )
            }
        }
    }
}

private const val HeroSupportingContentAlpha = 0.84f
private const val HeroTopScrimAlpha = 0.48f
private const val HeroBottomScrimAlpha = 0.9f
private const val HeroScrimClearStop = 0.42f
private const val RecommendationScrimAlpha = 0.78f
private const val LoadingTitleWidthFraction = 0.58f
private const val LoadingMetadataWidthFraction = 0.36f
private val LoadingTitleHeight = StreamCoreDimens.Spacing.ExtraLarge
private val LoadingTextHeight = StreamCoreDimens.Spacing.Large
private val LoadingBodyHeight = StreamCoreDimens.Icon.TouchTarget
private const val OverviewEntranceDelayMillis = 80
private const val RecommendationsEntranceDelayMillis = 150

@PreviewMobile
@Composable
private fun MobileDetailsScreenDarkPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
            ),
            onAction = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileDetailsScreenLightPreview() {
    StreamCoreTheme(darkTheme = false) {
        MobileDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
            ),
            onAction = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileDetailsScreenLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileDetailsScreen(
            state = DetailsUiState(isLoading = true),
            onAction = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileDetailsScreenErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileDetailsScreen(
            state = DetailsUiState(isLoading = false),
            onAction = {},
        )
    }
}
