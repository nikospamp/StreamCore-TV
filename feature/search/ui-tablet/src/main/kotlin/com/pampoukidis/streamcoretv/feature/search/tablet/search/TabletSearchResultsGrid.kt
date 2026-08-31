package com.pampoukidis.streamcoretv.feature.search.tablet.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import com.pampoukidis.streamcoretv.feature.search.tablet.R

@Composable
internal fun TabletSearchResultsGrid(
    items: List<ContentModel>,
    onContentSelected: (ContentModel) -> Unit,
    state: LazyGridState,
    modifier: Modifier = Modifier,
    showOfflineNotice: Boolean = false,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = StreamCoreDimens.Tablet.Browse.PosterWidth),
        state = state,
        modifier = modifier.testTag(SearchTestTags.ResultsGrid),
        contentPadding = PaddingValues(
            start = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            top = StreamCoreDimens.Spacing.Large,
            end = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            bottom = StreamCoreDimens.Tablet.Screen.VerticalPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
    ) {
        if (showOfflineNotice) {
            item(
                key = OfflineNoticeKey,
                span = { GridItemSpan(maxLineSpan) },
                contentType = OfflineNoticeContentType,
            ) {
                OfflineNotice()
            }
        }
        item(
            key = ResultsHeadingKey,
            span = { GridItemSpan(maxLineSpan) },
            contentType = ResultsHeadingContentType,
        ) {
            val resultsLoadedDescription = stringResource(R.string.search_results_loaded)
            Text(
                text = stringResource(R.string.search_results_heading),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = resultsLoadedDescription
                },
            )
        }
        items(
            items = items,
            key = { content -> "${content.row}:${content.id}" },
            contentType = { ResultContentType },
        ) { content ->
            val contentKey = StreamCoreSharedKey.content(
                contentId = content.id,
                row = content.row,
            )
            SearchResultCard(
                content = content,
                onClick = { onContentSelected(content) },
                sharedElementScope = sharedElementScope.takeIf {
                    selectedContentKey == contentKey
                },
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    content: ContentModel,
    onClick: () -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    val shape = MaterialTheme.shapes.medium
    val openDetailsDescription = stringResource(
        R.string.search_content_description,
        content.title,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = openDetailsDescription }
            .clickable(onClick = onClick)
            .testTag(SearchTestTags.result(content.id)),
    ) {
        StreamCoreContentImage(
            imageUrl = content.poster,
            contentDescription = null,
            fallbackText = content.fallbackText(),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                .streamCoreSharedBounds(
                    sharedElementScope = sharedElementScope,
                    key = StreamCoreSharedKey.artwork(content.id, content.row),
                    clipShape = shape,
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.streamCoreSharedBounds(
                    sharedElementScope = sharedElementScope,
                    key = StreamCoreSharedKey.title(content.id, content.row),
                    clipShape = RoundedCornerShape(StreamCoreDimens.Spacing.Tiny),
                ),
            )
            Text(
                text = content.homeMetadataText(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OfflineNotice() {
    Text(
        text = stringResource(R.string.search_offline_notice),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(StreamCoreDimens.Spacing.Small))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(StreamCoreDimens.Spacing.Medium)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
internal fun TabletSearchLoadingGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = StreamCoreDimens.Tablet.Browse.PosterWidth),
        userScrollEnabled = false,
        modifier = modifier.testTag(SearchTestTags.Loading),
        contentPadding = PaddingValues(
            horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            vertical = StreamCoreDimens.Spacing.Large,
        ),
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
    ) {
        item(
            key = LoadingHeadingKey,
            span = { GridItemSpan(maxLineSpan) },
            contentType = ResultsHeadingContentType,
        ) {
            Text(
                text = stringResource(R.string.search_results_heading),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(
            count = LoadingItemCount,
            key = { index -> "loading:$index" },
            contentType = { LoadingContentType },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(SearchTitleSkeletonFraction)
                        .height(StreamCoreDimens.Spacing.Large)
                        .clip(RoundedCornerShape(StreamCoreDimens.Spacing.Tiny))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}

private const val LoadingItemCount = 8
private const val SearchTitleSkeletonFraction = 0.72f
private const val OfflineNoticeKey = "offline-notice"
private const val ResultsHeadingKey = "results-heading"
private const val LoadingHeadingKey = "loading-heading"
private const val OfflineNoticeContentType = "offline-notice"
private const val ResultsHeadingContentType = "results-heading"
private const val ResultContentType = "search-result"
private const val LoadingContentType = "search-loading"

@PreviewTablet
@Composable
private fun TabletSearchResultsGridPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchResultsGrid(
            items = SearchPreviewData.items,
            onContentSelected = {},
            state = rememberLazyGridState(),
        )
    }
}

@PreviewTablet
@Composable
private fun TabletSearchLoadingGridPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchLoadingGrid()
    }
}
