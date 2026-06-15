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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
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
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 12.dp),
        ) {
            DetailsHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (state.isLoading && state.content != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
            DetailsBody(
                state = state,
                onAction = onAction,
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
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(contentType = "hero") {
                    DetailsHero(
                        content = content,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item(contentType = "metadata") {
                    DetailsMetadata(
                        content = content,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item(contentType = "recommendations") {
                    RecommendationsRow(
                        recommendations = state.recommendations,
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
    modifier: Modifier = Modifier,
) {
    StreamCoreContentImage(
        imageUrl = content.backdrop ?: content.poster,
        contentDescription = content.title,
        fallbackText = content.fallbackText(),
        contentScale = ContentScale.Crop,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        fallbackTextStyle = MaterialTheme.typography.displayLarge,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun DetailsMetadata(
    content: ContentModel,
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${releaseYear(content.releaseDate)} · ${content.pgRatingName} · ${content.rating}/10",
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = recommendations,
                key = { it.id },
                contentType = { "recommendation" },
            ) { content ->
                RecommendationCard(
                    content = content,
                    onClick = {
                        onAction(DetailsAction.RecommendationSelected(content.id))
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .width(142.dp)
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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

@PreviewMobile
@Composable
private fun MobileDetailsScreenPreview() {
    StreamCoreTVTheme {
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