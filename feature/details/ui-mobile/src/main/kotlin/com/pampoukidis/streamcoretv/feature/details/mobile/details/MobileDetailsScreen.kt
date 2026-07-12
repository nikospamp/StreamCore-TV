package com.pampoukidis.streamcoretv.feature.details.mobile.details

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
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
import java.util.Calendar
import java.util.TimeZone

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
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = StreamCoreDimens.Mobile.Screen.VerticalPadding),
        ) {
            DetailsHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
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
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        when {
            !state.isLoading && content == null -> Text(
                text = "Unable to load details.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            content != null -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(contentType = "hero") {
                    DetailsHero(
                        content = content,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.padding(
                            horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                        ),
                    )
                }
                item(contentType = "metadata") {
                    DetailsMetadata(
                        content = content,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.padding(
                            horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
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
    }
}

@Composable
private fun DetailsHero(
    content: ContentModel,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    val heroShape = MaterialTheme.shapes.large

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
            .fillMaxWidth()
            .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio)
            .streamCoreSharedBounds(
                sharedElementScope = sharedElementScope,
                key = streamCoreArtworkSharedKey(
                    contentId = content.id,
                    row = content.row,
                ),
                clipShape = heroShape,
            ),
    )
}

@Composable
private fun DetailsMetadata(
    content: ContentModel,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.streamCoreSharedBounds(
                sharedElementScope = sharedElementScope,
                key = streamCoreTitleSharedKey(
                    contentId = content.id,
                    row = content.row,
                ),
                clipShape = RectangleShape,
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
                    text = "${releaseYear(content.releaseDate)} · ${content.rating}/10 · ${content.pgRatingName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = genreText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = StreamCoreDimens.Elevation.Low,
        modifier = modifier
            .width(StreamCoreDimens.Mobile.Details.RecommendationCardWidth)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            StreamCoreContentImage(
                imageUrl = content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                fallbackTextStyle = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                    .clip(MaterialTheme.shapes.medium),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Spacing.Medium,
                    vertical = StreamCoreDimens.Spacing.Small,
                ),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${content.rating}/10 · ${content.pgRatingName}",
                    style = MaterialTheme.typography.labelMedium,
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

@PreviewMobile
@Composable
private fun MobileDetailsScreenPreview() {
    StreamCoreTheme {
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
