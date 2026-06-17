package com.pampoukidis.streamcoretv.feature.details.tv.details

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreLoadingChip
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreDelayedEntrance
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreArtworkSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreTitleSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import java.util.Calendar
import java.util.TimeZone

@Composable
fun TvDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val backFocusRequester = remember { FocusRequester() }
    val contentId = state.content?.id

    LaunchedEffect(contentId) {
        backFocusRequester.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = StreamCoreDimens.Tv.ScreenVerticalPadding),
        ) {
            DetailsHeader(
                isLoading = state.isLoading,
                backFocusRequester = backFocusRequester,
                onAction = onAction,
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding,
                ),
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
    backFocusRequester: FocusRequester,
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
                .testTag(DetailsTestTags.BackButton),
        )
        StreamCoreTvButton(
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Tv.ScreenVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(36.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(contentType = "summary") {
                    SummarySection(
                        content = content,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.padding(
                            horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding,
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
                textStyle = MaterialTheme.typography.labelLarge,
                indicatorSize = 18.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 12.dp,
                        end = StreamCoreDimens.Tv.ScreenHorizontalPadding,
                    ),
            )
        }
    }
}

@Composable
private fun SummarySection(
    content: ContentModel,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(34.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        DetailsHero(
            content = content,
            sharedElementScope = sharedElementScope,
            modifier = Modifier
                .weight(0.56f)
                .aspectRatio(16f / 9f),
        )
        DetailsMetadata(
            content = content,
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
    val heroShape = RoundedCornerShape(16.dp)

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
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
private fun RecommendationsRow(
    recommendations: List<ContentModel>,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding,
                vertical = StreamCoreDimens.Tv.FocusBorderPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
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
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) 8.dp else 2.dp,
        border = if (isFocused) {
            BorderStroke(
                width = StreamCoreDimens.Tv.FocusBorderWidth,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
        modifier = modifier
            .width(320.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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

@PreviewTV
@Composable
private fun TvDetailsScreenPreview() {
    StreamCoreTVTheme {
        TvDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = DetailsPreviewData.content,
                recommendations = DetailsPreviewData.recommendations,
            ),
            onAction = {},
        )
    }
}
