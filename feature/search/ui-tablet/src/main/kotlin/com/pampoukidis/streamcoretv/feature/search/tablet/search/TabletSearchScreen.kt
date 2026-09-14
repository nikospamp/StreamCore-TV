package com.pampoukidis.streamcoretv.feature.search.tablet.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHistoryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import com.pampoukidis.streamcoretv.feature.search.tablet.R

@Composable
fun TabletSearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(SearchTestTags.Screen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            TabletSearchField(
                query = state.query,
                onQueryChanged = { query -> onAction(SearchAction.QueryChanged(query)) },
                onClear = { onAction(SearchAction.ClearQuery) },
                onSubmit = { onAction(SearchAction.SubmitQuery) },
                focusRequester = focusRequester,
                modifier = Modifier
                    .padding(
                        start = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                        top = StreamCoreDimens.Tablet.Screen.VerticalPadding,
                        end = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                        bottom = StreamCoreDimens.Spacing.Small,
                    )
                    .width(StreamCoreDimens.Form.WideContentMaxWidth),
            )
            Crossfade(
                targetState = state.content,
                animationSpec = tween(ContentCrossfadeMillis),
                label = "TabletSearchContentState",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    SearchContentState.Discovery -> SearchDiscoveryContent(
                        recentQueries = state.recentQueries,
                        trending = state.trending,
                        onAction = onAction,
                    )

                    SearchContentState.Searching -> Box(modifier = Modifier.fillMaxSize())
                    SearchContentState.Loading -> TabletSearchLoadingGrid(
                        modifier = Modifier.fillMaxSize(),
                    )

                    is SearchContentState.Results -> TabletSearchResultsGrid(
                        items = contentState.items,
                        onContentSelected = { content ->
                            onAction(SearchAction.ResultSelected(content, sourceArtworkUrl = content.poster))
                        },
                        state = gridState,
                        showOfflineNotice = state.showOfflineNotice,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.fillMaxSize(),
                    )

                    is SearchContentState.Empty -> SearchMessageState(
                        title = stringResource(R.string.search_empty_title, contentState.query),
                        body = stringResource(R.string.search_empty_body),
                        action = stringResource(R.string.search_clear_query),
                        onAction = { onAction(SearchAction.ClearQuery) },
                        testTag = SearchTestTags.Empty,
                    )

                    is SearchContentState.Failure -> SearchFailureState(
                        error = contentState.error,
                        onRetry = { onAction(SearchAction.Retry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchDiscoveryContent(
    recentQueries: List<String>,
    trending: List<ContentModel>,
    onAction: (SearchAction) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            top = StreamCoreDimens.Spacing.Large,
            end = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            bottom = StreamCoreDimens.Tablet.Screen.VerticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (recentQueries.isNotEmpty()) {
            item(key = RecentHeadingKey, contentType = HeadingContentType) {
                DiscoveryHeading(
                    title = stringResource(R.string.search_recent_heading),
                    action = stringResource(R.string.search_clear_recent),
                    onAction = { onAction(SearchAction.ClearRecent) },
                )
            }
            items(
                items = recentQueries,
                key = { query -> query },
                contentType = { RecentContentType },
            ) { query ->
                RecentQueryRow(
                    query = query,
                    onSelected = { onAction(SearchAction.RecentSelected(query)) },
                    onRemoved = { onAction(SearchAction.RecentRemoved(query)) },
                )
            }
        }

        if (trending.isNotEmpty()) {
            item(key = TrendingSpacerKey, contentType = SpacerContentType) {
                Spacer(modifier = Modifier.size(StreamCoreDimens.Spacing.Small))
            }
            item(key = TrendingHeadingKey, contentType = HeadingContentType) {
                DiscoveryHeading(title = stringResource(R.string.search_trending_heading))
            }
            items(
                items = trending,
                key = { content -> "${content.row}:${content.id}" },
                contentType = { TrendingContentType },
            ) { content ->
                TrendingContentRow(
                    content = content,
                    onSelected = {
                        onAction(
                            SearchAction.TrendingSelected(
                                content = content,
                                sourceArtworkUrl = content.backdrop ?: content.poster,
                            ),
                        )
                    },
                )
            }
        }

        if (recentQueries.isEmpty() && trending.isEmpty()) {
            item(key = DiscoveryHelperKey, contentType = HelperContentType) {
                Text(
                    text = stringResource(R.string.search_discovery_helper),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = StreamCoreDimens.Spacing.ExtraLarge),
                )
            }
        }
    }
}

@Composable
private fun DiscoveryHeading(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (action != null) {
            StreamCoreTextButton(text = action, onClick = onAction, enabled = true)
        }
    }
}

@Composable
private fun RecentQueryRow(
    query: String,
    onSelected: () -> Unit,
    onRemoved: () -> Unit,
) {
    val removeDescription = stringResource(R.string.search_remove_recent, query)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .testTag(SearchTestTags.recent(query)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(StreamCoreDimens.Icon.TouchTarget),
        ) {
            StreamCoreHistoryIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StreamCoreCloseButton(
            onClick = onRemoved,
            enabled = true,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = removeDescription,
            modifier = Modifier.testTag(SearchTestTags.removeRecent(query)),
        )
    }
}

@Composable
private fun TrendingContentRow(
    content: ContentModel,
    onSelected: () -> Unit,
) {
    val openDetailsDescription = stringResource(
        R.string.search_content_description,
        content.title,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = openDetailsDescription }
            .clickable(onClick = onSelected)
            .padding(vertical = StreamCoreDimens.Spacing.Tiny),
    ) {
        StreamCoreContentImage(
            imageUrl = content.backdrop ?: content.poster,
            contentDescription = null,
            fallbackText = content.fallbackText(),
            modifier = Modifier
                .width(StreamCoreDimens.Tablet.Browse.LandscapeWidth)
                .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio)
                .clip(MaterialTheme.shapes.medium),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.homeMetadataText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchFailureState(
    error: AppError,
    onRetry: () -> Unit,
) {
    val isOffline = error is AppError.Network || error is AppError.Timeout
    SearchMessageState(
        title = stringResource(
            if (isOffline) {
                R.string.search_failure_offline_title
            } else {
                R.string.search_failure_generic_title
            },
        ),
        body = stringResource(R.string.search_failure_body),
        action = stringResource(R.string.search_try_again),
        onAction = onRetry,
        testTag = SearchTestTags.Failure,
    )
}

@Composable
private fun SearchMessageState(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    testTag: String,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(StreamCoreDimens.Tablet.Screen.HorizontalPadding)
            .testTag(testTag)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StreamCoreButton(text = action, onClick = onAction, enabled = true)
        }
    }
}

private const val ContentCrossfadeMillis = 200
private const val RecentHeadingKey = "recent-heading"
private const val TrendingHeadingKey = "trending-heading"
private const val TrendingSpacerKey = "trending-spacer"
private const val DiscoveryHelperKey = "discovery-helper"
private const val HeadingContentType = "search-heading"
private const val RecentContentType = "search-recent"
private const val TrendingContentType = "search-trending"
private const val SpacerContentType = "search-spacer"
private const val HelperContentType = "search-helper"

@PreviewTablet
@Composable
private fun TabletSearchDiscoveryPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchScreenPreview(state = SearchPreviewData.discovery)
    }
}

@PreviewTablet
@Composable
private fun TabletSearchResultsPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchScreenPreview(state = SearchPreviewData.results)
    }
}

@PreviewTablet
@Composable
private fun TabletSearchLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchScreenPreview(state = SearchPreviewData.loading)
    }
}

@PreviewTablet
@Composable
private fun TabletSearchEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchScreenPreview(state = SearchPreviewData.empty)
    }
}

@PreviewTablet
@Composable
private fun TabletSearchFailurePreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchScreenPreview(state = SearchPreviewData.failure)
    }
}

@PreviewTablet
@Composable
private fun TabletSearchLongTextPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletSearchScreenPreview(state = SearchPreviewData.longLocalized)
    }
}

@Composable
private fun TabletSearchScreenPreview(state: SearchUiState) {
    TabletSearchScreen(
        state = state,
        onAction = {},
        gridState = rememberLazyGridState(),
    )
}
