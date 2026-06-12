package com.pampoukidis.streamcoretv.feature.home.tablet.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags

@Composable
fun TabletHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 24.dp),
        ) {
            HomeHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            if (state.isLoading && state.rows.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
            }
            TabletHomeBody(
                state = state,
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    isLoading: Boolean,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Featured movies and series for your profile",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StreamCoreTextButton(
            text = "Refresh",
            onClick = { onAction(HomeAction.Refresh) },
            enabled = !isLoading,
            modifier = Modifier.testTag(HomeTestTags.RefreshButton),
        )
    }
}

@Composable
private fun TabletHomeBody(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.rows.isEmpty() -> CircularProgressIndicator()
            state.rows.isEmpty() -> Text(
                text = "No content available.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.rows,
                    key = { it.id },
                    contentType = { it.style },
                ) { row ->
                    TabletContentRow(
                        row = row,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletContentRow(
    row: RowModel,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.style },
            ) { index, content ->
                TabletContentCard(
                    rowId = row.id,
                    content = content,
                    style = row.style,
                    rank = index + 1,
                    onClick = {
                        onAction(HomeAction.ContentSelected(content))
                    },
                )
            }
        }
    }
}

@Composable
private fun TabletContentCard(
    rowId: String,
    content: ContentModel,
    style: RowStyle,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = style.tabletCardSpec()
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { it.name }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .width(spec.width)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(spec.aspectRatio)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Text(
                    text = content.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (style == RowStyle.TopTen) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spec.showDescription) {
                    Text(
                        text = content.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = genreText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun RowStyle.tabletCardSpec(): TabletCardSpec {
    return when (this) {
        RowStyle.Carousel -> TabletCarouselCardSpec
        RowStyle.Poster -> TabletPosterCardSpec
        RowStyle.Landscape -> TabletLandscapeCardSpec
        RowStyle.TopTen -> TabletTopTenCardSpec
    }
}

private data class TabletCardSpec(
    val width: Dp,
    val aspectRatio: Float,
    val showDescription: Boolean,
)

private val TabletCarouselCardSpec = TabletCardSpec(
    width = 420.dp,
    aspectRatio = 16f / 9f,
    showDescription = true,
)
private val TabletPosterCardSpec = TabletCardSpec(
    width = 190.dp,
    aspectRatio = 2f / 3f,
    showDescription = false,
)
private val TabletLandscapeCardSpec = TabletCardSpec(
    width = 300.dp,
    aspectRatio = 16f / 9f,
    showDescription = true,
)
private val TabletTopTenCardSpec = TabletCardSpec(
    width = 220.dp,
    aspectRatio = 2f / 3f,
    showDescription = false,
)

@PreviewTablet
@Composable
private fun TabletHomeScreenPreview() {
    StreamCoreTVTheme {
        TabletHomeScreen(
            state = HomeUiState(
                isLoading = false,
                rows = HomePreviewData.rows,
            ),
            onAction = {},
        )
    }
}