package com.pampoukidis.streamcoretv.feature.search.web.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtwork
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebContentCard
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.search.ui_web.generated.resources.Res
import streamcoretv.feature.search.ui_web.generated.resources.web_search_clear
import streamcoretv.feature.search.ui_web.generated.resources.web_search_clear_recent
import streamcoretv.feature.search.ui_web.generated.resources.web_search_clear_search
import streamcoretv.feature.search.ui_web.generated.resources.web_search_discovery_empty
import streamcoretv.feature.search.ui_web.generated.resources.web_search_empty_message
import streamcoretv.feature.search.ui_web.generated.resources.web_search_empty_title
import streamcoretv.feature.search.ui_web.generated.resources.web_search_failure_message
import streamcoretv.feature.search.ui_web.generated.resources.web_search_failure_title
import streamcoretv.feature.search.ui_web.generated.resources.web_search_loading_results
import streamcoretv.feature.search.ui_web.generated.resources.web_search_offline_notice
import streamcoretv.feature.search.ui_web.generated.resources.web_search_open_details
import streamcoretv.feature.search.ui_web.generated.resources.web_search_recent_heading
import streamcoretv.feature.search.ui_web.generated.resources.web_search_remove
import streamcoretv.feature.search.ui_web.generated.resources.web_search_remove_recent_description
import streamcoretv.feature.search.ui_web.generated.resources.web_search_results_heading
import streamcoretv.feature.search.ui_web.generated.resources.web_search_searching
import streamcoretv.feature.search.ui_web.generated.resources.web_search_subtitle
import streamcoretv.feature.search.ui_web.generated.resources.web_search_title
import streamcoretv.feature.search.ui_web.generated.resources.web_search_trending_heading
import streamcoretv.feature.search.ui_web.generated.resources.web_search_try_again

@Composable
fun WebSearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBack = LocalWebSearchBackHandler.current
    val returnFocusTarget = remember(state, returnFocusKey) {
        resolveWebSearchReturnFocus(state = state, key = returnFocusKey)
    }
    var initialFieldFocusPending by rememberSaveable {
        mutableStateOf(returnFocusKey == null)
    }

    LaunchedEffect(returnFocusKey) {
        if (returnFocusKey != null) {
            initialFieldFocusPending = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .webEscape(onBack)
            .testTag(SearchTestTags.Screen),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            modifier = Modifier.fillMaxSize(),
        ) {
            SearchHeading()
            WebSearchFieldRow(
                state = state,
                requestFocus = initialFieldFocusPending ||
                    returnFocusTarget is WebSearchReturnFocusTarget.Field,
                onAction = onAction,
                onEscape = onBack,
                onFocusRequestConsumed = {
                    initialFieldFocusPending = false
                    if (returnFocusTarget is WebSearchReturnFocusTarget.Field) {
                        returnFocusKey?.let(onReturnFocusConsumed)
                    }
                },
            )
            when (val content = state.content) {
                SearchContentState.Discovery -> WebSearchDiscovery(
                    recentQueries = state.recentQueries,
                    trending = state.trending,
                    selectedContentKey = selectedContentKey,
                    returnFocusKey = returnFocusKey,
                    returnFocusTarget = returnFocusTarget,
                    onAction = onAction,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                    modifier = Modifier.weight(1f),
                )

                SearchContentState.Searching -> WebSearchLoading(
                    title = stringResource(Res.string.web_search_searching),
                    modifier = Modifier.weight(1f),
                )

                SearchContentState.Loading -> WebSearchLoading(
                    title = stringResource(Res.string.web_search_loading_results),
                    modifier = Modifier.weight(1f),
                )

                is SearchContentState.Results -> WebSearchResults(
                    items = content.items,
                    showOfflineNotice = state.showOfflineNotice,
                    selectedContentKey = selectedContentKey,
                    returnFocusKey = returnFocusKey,
                    returnFocusTarget = returnFocusTarget,
                    onAction = onAction,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                    modifier = Modifier.weight(1f),
                )

                is SearchContentState.Empty -> WebSearchMessage(
                    title = stringResource(Res.string.web_search_empty_title, content.query),
                    message = stringResource(Res.string.web_search_empty_message),
                    actionLabel = stringResource(Res.string.web_search_clear_search),
                    onAction = { onAction(SearchAction.ClearQuery) },
                    testTag = SearchTestTags.Empty,
                    modifier = Modifier.weight(1f),
                )

                is SearchContentState.Failure -> WebSearchMessage(
                    title = stringResource(Res.string.web_search_failure_title),
                    message = stringResource(Res.string.web_search_failure_message),
                    actionLabel = stringResource(Res.string.web_search_try_again),
                    onAction = { onAction(SearchAction.Retry) },
                    testTag = SearchTestTags.Failure,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SearchHeading() {
    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
        Text(
            text = stringResource(Res.string.web_search_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.web_search_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WebSearchFieldRow(
    state: SearchUiState,
    requestFocus: Boolean,
    onAction: (SearchAction) -> Unit,
    onEscape: () -> Unit,
    onFocusRequestConsumed: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        WebSearchTextField(
            value = state.query,
            enabled = true,
            requestFocus = requestFocus,
            onValueChange = { value -> onAction(SearchAction.QueryChanged(value)) },
            onSubmitCommittedValue = { committedValue ->
                submitCommittedSearch(committedValue = committedValue, onAction = onAction)
            },
            onEscape = onEscape,
            onFocusRequestConsumed = onFocusRequestConsumed,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = StreamCoreDimens.Web.Search.FieldMaxWidth),
        )
        if (state.query.isNotEmpty()) {
            StreamCoreWebButton(
                text = stringResource(Res.string.web_search_clear),
                onClick = { onAction(SearchAction.ClearQuery) },
                enabled = true,
                variant = StreamCoreWebButtonVariant.Tertiary,
                modifier = Modifier.testTag(SearchTestTags.ClearQuery),
            )
        }
    }
}

@Composable
private fun WebSearchDiscovery(
    recentQueries: List<String>,
    trending: List<ContentModel>,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    returnFocusTarget: WebSearchReturnFocusTarget,
    onAction: (SearchAction) -> Unit,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier,
) {
    if (recentQueries.isEmpty() && trending.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.web_search_discovery_empty),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (recentQueries.isNotEmpty()) {
            item(key = RecentSectionKey, contentType = RecentSectionContentType) {
                WebRecentSearches(recentQueries = recentQueries, onAction = onAction)
            }
        }
        if (trending.isNotEmpty()) {
            item(key = TrendingSectionKey, contentType = TrendingSectionContentType) {
                WebTrendingSearches(
                    items = trending,
                    selectedContentKey = selectedContentKey,
                    returnFocusKey = returnFocusKey,
                    returnFocusTarget = returnFocusTarget,
                    onAction = onAction,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                )
            }
        }
    }
}

@Composable
private fun WebRecentSearches(
    recentQueries: List<String>,
    onAction: (SearchAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier.testTag(SearchTestTags.RecentList),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.web_search_recent_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            StreamCoreWebButton(
                text = stringResource(Res.string.web_search_clear_recent),
                onClick = { onAction(SearchAction.ClearRecent) },
                variant = StreamCoreWebButtonVariant.Tertiary,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            contentPadding = PaddingValues(vertical = StreamCoreDimens.Spacing.Tiny),
        ) {
            items(
                items = recentQueries,
                key = { query -> query },
                contentType = { RecentQueryContentType },
            ) { query ->
                val removeDescription = stringResource(
                    Res.string.web_search_remove_recent_description,
                    query,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
                    StreamCoreWebButton(
                        text = query,
                        onClick = { onAction(SearchAction.RecentSelected(query)) },
                        variant = StreamCoreWebButtonVariant.Secondary,
                        modifier = Modifier
                            .widthIn(max = StreamCoreDimens.Web.Search.RecentQueryMaxWidth)
                            .testTag(SearchTestTags.recent(query)),
                    )
                    StreamCoreWebButton(
                        text = stringResource(Res.string.web_search_remove),
                        onClick = { onAction(SearchAction.RecentRemoved(query)) },
                        variant = StreamCoreWebButtonVariant.Tertiary,
                        modifier = Modifier
                            .semantics { contentDescription = removeDescription }
                            .testTag(SearchTestTags.removeRecent(query)),
                    )
                }
            }
        }
    }
}

@Composable
private fun WebTrendingSearches(
    items: List<ContentModel>,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    returnFocusTarget: WebSearchReturnFocusTarget,
    onAction: (SearchAction) -> Unit,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
) {
    val rowState = rememberLazyListState()
    val focusRequesters = remember(items) {
        items.associate { content -> content.webSearchFocusKey() to FocusRequester() }
    }
    val target = returnFocusTarget as? WebSearchReturnFocusTarget.Trending
    val targetRequester = target?.let { targetValue ->
        items.getOrNull(targetValue.index)?.webSearchFocusKey()?.let(focusRequesters::get)
    }

    LaunchedEffect(target, returnFocusKey, targetRequester) {
        val focusKey = returnFocusKey ?: return@LaunchedEffect
        val targetValue = target ?: return@LaunchedEffect
        val requester = targetRequester ?: return@LaunchedEffect
        rowState.scrollToItem(targetValue.index)
        if (requester.requestFocusWhenReady()) {
            onReturnFocusConsumed(focusKey)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier.testTag(SearchTestTags.TrendingList),
    ) {
        Text(
            text = stringResource(Res.string.web_search_trending_heading),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            contentPadding = PaddingValues(vertical = StreamCoreDimens.Spacing.Tiny),
        ) {
            listItemsIndexed(
                items = items,
                key = { _, content -> "${content.row}:${content.id}" },
                contentType = { _, _ -> TrendingContentType },
            ) { index, content ->
                val focusKey = content.webSearchFocusKey()
                WebSearchContentTile(
                    content = content,
                    imageUrl = content.backdrop ?: content.poster,
                    selected = content.matchesWebSearchFocusKey(selectedContentKey),
                    aspectRatio = StreamCoreDimens.Artwork.LandscapeAspectRatio,
                    requestWidthPx = TrendingRequestWidthPx,
                    requestHeightPx = TrendingRequestHeightPx,
                    onClick = { onAction(SearchAction.TrendingSelected(content)) },
                    modifier = Modifier.width(StreamCoreDimens.Web.Search.TrendingCardWidth),
                    interactionModifier = Modifier
                        .focusRequester(requireNotNull(focusRequesters[focusKey]))
                        .webSearchFocusOrder(
                            left = items.getOrNull(index - 1)?.webSearchFocusKey()?.let(focusRequesters::get),
                            right = items.getOrNull(index + 1)?.webSearchFocusKey()?.let(focusRequesters::get),
                        )
                        .testTag(SearchTestTags.result(content.id)),
                )
            }
        }
    }
}

@Composable
private fun WebSearchResults(
    items: List<ContentModel>,
    showOfflineNotice: Boolean,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    returnFocusTarget: WebSearchReturnFocusTarget,
    onAction: (SearchAction) -> Unit,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (showOfflineNotice) {
            Text(
                text = stringResource(Res.string.web_search_offline_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(StreamCoreDimens.Spacing.Medium)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(OfflineNoticeTestTag),
            )
        }
        Text(
            text = stringResource(Res.string.web_search_results_heading),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columnCount = remember(maxWidth) {
                val cardSlotWidth = StreamCoreDimens.Web.Search.ResultCardMinWidth +
                    StreamCoreDimens.Spacing.Large
                maxOf(1, (maxWidth / cardSlotWidth).toInt())
            }
            val gridState = rememberLazyGridState()
            val focusRequesters = remember(items) {
                items.associate { content -> content.webSearchFocusKey() to FocusRequester() }
            }
            val target = returnFocusTarget as? WebSearchReturnFocusTarget.Results
            val targetRequester = target?.let { targetValue ->
                items.getOrNull(targetValue.index)?.webSearchFocusKey()?.let(focusRequesters::get)
            }

            LaunchedEffect(target, returnFocusKey, targetRequester, columnCount) {
                val focusKey = returnFocusKey ?: return@LaunchedEffect
                val targetValue = target ?: return@LaunchedEffect
                val requester = targetRequester ?: return@LaunchedEffect
                gridState.scrollToItem(targetValue.index)
                if (requester.requestFocusWhenReady()) {
                    onReturnFocusConsumed(focusKey)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                contentPadding = PaddingValues(
                    top = StreamCoreDimens.Spacing.Tiny,
                    bottom = StreamCoreDimens.Spacing.ExtraLarge,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(SearchTestTags.ResultsGrid),
            ) {
                gridItemsIndexed(
                    items = items,
                    key = { _, content -> "${content.row}:${content.id}" },
                    contentType = { _, _ -> ResultContentType },
                ) { index, content ->
                    val focusKey = content.webSearchFocusKey()
                    WebSearchContentTile(
                        content = content,
                        imageUrl = content.poster,
                        selected = content.matchesWebSearchFocusKey(selectedContentKey),
                        aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
                        requestWidthPx = PosterRequestWidthPx,
                        requestHeightPx = PosterRequestHeightPx,
                        onClick = { onAction(SearchAction.ResultSelected(content)) },
                        modifier = Modifier.fillMaxWidth(),
                        interactionModifier = Modifier
                            .focusRequester(requireNotNull(focusRequesters[focusKey]))
                            .webSearchFocusOrder(
                                left = items.getOrNull(index - 1)
                                    ?.takeIf { index % columnCount != 0 }
                                    ?.webSearchFocusKey()
                                    ?.let(focusRequesters::get),
                                right = items.getOrNull(index + 1)
                                    ?.takeIf { (index + 1) % columnCount != 0 }
                                    ?.webSearchFocusKey()
                                    ?.let(focusRequesters::get),
                                up = items.getOrNull(index - columnCount)
                                    ?.webSearchFocusKey()
                                    ?.let(focusRequesters::get),
                                down = items.getOrNull(index + columnCount)
                                    ?.webSearchFocusKey()
                                    ?.let(focusRequesters::get),
                            )
                            .testTag(SearchTestTags.result(content.id)),
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSearchContentTile(
    content: ContentModel,
    imageUrl: String?,
    selected: Boolean,
    aspectRatio: Float,
    requestWidthPx: Int,
    requestHeightPx: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionModifier: Modifier,
) {
    val openDetailsDescription = stringResource(
        Res.string.web_search_open_details,
        content.title,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier,
    ) {
        StreamCoreWebContentCard(
            onClick = onClick,
            selected = selected,
            aspectRatio = aspectRatio,
            modifier = interactionModifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = openDetailsDescription
                },
        ) {
            StreamCoreWebArtwork(
                imageUrl = imageUrl,
                contentDescription = null,
                fallbackText = content.fallbackText(),
                requestWidthPx = requestWidthPx,
                requestHeightPx = requestHeightPx,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WebSearchLoading(
    title: String,
    modifier: Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(SearchTestTags.Loading)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        ) {
            items(
                count = LoadingItemCount,
                key = { index -> "search-loading:$index" },
                contentType = { LoadingContentType },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                    modifier = Modifier.width(StreamCoreDimens.Web.Search.ResultCardMinWidth),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(StreamCoreDimens.Web.Search.LoadingPosterHeight)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.medium,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(LoadingTitleFraction)
                            .height(StreamCoreDimens.Spacing.Large)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSearchMessage(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StreamCoreWebButton(text = actionLabel, onClick = onAction)
        }
    }
}

private fun Modifier.webSearchFocusOrder(
    left: FocusRequester? = null,
    right: FocusRequester? = null,
    up: FocusRequester? = null,
    down: FocusRequester? = null,
): Modifier {
    return focusProperties {
        if (left != null) this.left = left
        if (right != null) this.right = right
        if (up != null) this.up = up
        if (down != null) this.down = down
    }
}

private suspend fun FocusRequester.requestFocusWhenReady(): Boolean {
    repeat(FocusRequestAttempts) {
        withFrameNanos { }
        if (requestFocus()) {
            return true
        }
    }
    return false
}

private const val LoadingTitleFraction = 0.72f
private const val LoadingItemCount = 6
private const val FocusRequestAttempts = 4
private const val TrendingRequestWidthPx = 600
private const val TrendingRequestHeightPx = 338
private const val PosterRequestWidthPx = 360
private const val PosterRequestHeightPx = 540
private const val RecentSectionKey = "search:recent-section"
private const val TrendingSectionKey = "search:trending-section"
private const val RecentSectionContentType = "search-recent-section"
private const val TrendingSectionContentType = "search-trending-section"
private const val RecentQueryContentType = "search-recent-query"
private const val TrendingContentType = "search-trending-content"
private const val ResultContentType = "search-result"
private const val LoadingContentType = "search-loading"
private const val OfflineNoticeTestTag = "search:offline"

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchDiscoveryPreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.discovery)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchLoadingPreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.Loading))
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchContentPreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.Content))
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchEmptyPreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.Empty))
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchOfflinePreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.Offline))
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchErrorPreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.Error))
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebSearchLongTextPreview() {
    WebSearchPreview(state = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.LongText))
}

@Composable
private fun WebSearchPreview(state: SearchUiState) {
    StreamCoreTheme(darkTheme = true) {
        WebSearchScreen(
            state = state,
            onAction = {},
            selectedContentKey = null,
            returnFocusKey = null,
            onReturnFocusConsumed = {},
        )
    }
}
