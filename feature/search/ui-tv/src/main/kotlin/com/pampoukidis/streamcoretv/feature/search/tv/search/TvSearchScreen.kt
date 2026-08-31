package com.pampoukidis.streamcoretv.feature.search.tv.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvContentCard
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import com.pampoukidis.streamcoretv.feature.search.tv.R

@Composable
fun TvSearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onKeyboardRequested: () -> Unit,
    fieldFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    returnFocusKey: String? = null,
    onReturnFocusConsumed: (String) -> Unit = {},
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    LaunchedEffect(state.content, returnFocusKey) {
        val focusKey = returnFocusKey ?: return@LaunchedEffect
        val shouldUseFieldFallback = state.content is SearchContentState.Empty ||
                state.content is SearchContentState.Failure
        if (shouldUseFieldFallback && fieldFocusRequester.requestFocusWhenReady()) {
            onReturnFocusConsumed(focusKey)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(SearchTestTags.Screen),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                    top = StreamCoreDimens.Tv.Screen.VerticalPadding,
                    end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                    bottom = StreamCoreDimens.Tv.Screen.VerticalPadding,
                ),
        ) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TvSearchField(
                query = state.query,
                onQueryChanged = { query -> onAction(SearchAction.QueryChanged(query)) },
                onClear = { onAction(SearchAction.ClearQuery) },
                onSubmit = { onAction(SearchAction.SubmitQuery) },
                onKeyboardRequested = onKeyboardRequested,
                focusRequester = fieldFocusRequester,
            )
            Crossfade(
                targetState = state.content,
                animationSpec = tween(ContentCrossfadeMillis),
                label = "TvSearchContentState",
                modifier = Modifier.weight(1f),
            ) { contentState ->
                when (contentState) {
                    SearchContentState.Discovery -> TvSearchDiscovery(
                        recentQueries = state.recentQueries,
                        trending = state.trending,
                        onAction = onAction,
                        selectedContentKey = selectedContentKey,
                        returnFocusKey = returnFocusKey,
                        fallbackFocusRequester = fieldFocusRequester,
                        onReturnFocusConsumed = onReturnFocusConsumed,
                        sharedElementScope = sharedElementScope,
                    )

                    SearchContentState.Searching -> TvSearchStatus(
                        text = stringResource(R.string.search_searching),
                    )

                    SearchContentState.Loading -> TvSearchLoading()
                    is SearchContentState.Results -> TvSearchResults(
                        items = contentState.items,
                        showOfflineNotice = state.showOfflineNotice,
                        onSelected = { content ->
                            onAction(SearchAction.ResultSelected(content))
                        },
                        selectedContentKey = selectedContentKey,
                        returnFocusKey = returnFocusKey,
                        fallbackFocusRequester = fieldFocusRequester,
                        onReturnFocusConsumed = onReturnFocusConsumed,
                        sharedElementScope = sharedElementScope,
                    )

                    is SearchContentState.Empty -> TvSearchMessage(
                        title = stringResource(R.string.search_empty_title, contentState.query),
                        body = stringResource(R.string.search_empty_body),
                        action = stringResource(R.string.search_clear_query),
                        onAction = { onAction(SearchAction.ClearQuery) },
                        testTag = SearchTestTags.Empty,
                    )

                    is SearchContentState.Failure -> TvSearchMessage(
                        title = stringResource(R.string.search_failure_title),
                        body = stringResource(R.string.search_failure_body),
                        action = stringResource(R.string.search_try_again),
                        onAction = { onAction(SearchAction.Retry) },
                        testTag = SearchTestTags.Failure,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onKeyboardRequested: () -> Unit,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { StreamCoreSearchIcon() },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                showKeyboardOnFocus = false,
            ),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier
                .width(StreamCoreDimens.Tv.Search.FieldMaxWidth)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .onPreviewKeyEvent { event ->
                    val requestsKeyboard = isFocused &&
                            event.type == KeyEventType.KeyDown &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    if (requestsKeyboard) {
                        onKeyboardRequested()
                    }
                    false
                }
                .testTag(SearchTestTags.Field),
        )
        if (query.isNotEmpty()) {
            StreamCoreTvButton(
                text = stringResource(R.string.search_clear_query),
                onClick = onClear,
                enabled = true,
                variant = StreamCoreTvButtonVariant.Tertiary,
                modifier = Modifier.testTag(SearchTestTags.ClearQuery),
            )
        }
    }
}

@Composable
private fun TvSearchDiscovery(
    recentQueries: List<String>,
    trending: List<ContentModel>,
    onAction: (SearchAction) -> Unit,
    selectedContentKey: String?,
    returnFocusKey: String?,
    fallbackFocusRequester: FocusRequester,
    onReturnFocusConsumed: (String) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        contentPadding = PaddingValues(bottom = StreamCoreDimens.Tv.Screen.VerticalPadding),
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer(),
    ) {
        if (recentQueries.isNotEmpty()) {
            item(key = "recent", contentType = "recent") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                    modifier = Modifier.testTag(SearchTestTags.RecentList),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            StreamCoreDimens.Spacing.Large,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.search_recent_heading),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        StreamCoreTvButton(
                            text = stringResource(R.string.search_clear_recent),
                            onClick = { onAction(SearchAction.ClearRecent) },
                            enabled = true,
                            variant = StreamCoreTvButtonVariant.Tertiary,
                        )
                    }
                    LazyRow(
                        modifier = Modifier.focusRestorer(),
                        horizontalArrangement = Arrangement.spacedBy(
                            StreamCoreDimens.Spacing.Medium,
                        ),
                        contentPadding = PaddingValues(
                            vertical = StreamCoreDimens.Tv.Focus.BorderPadding,
                        ),
                    ) {
                        items(
                            items = recentQueries,
                            key = { query -> query },
                            contentType = { "recent-query" },
                        ) { query ->
                            StreamCoreTvButton(
                                text = query,
                                onClick = { onAction(SearchAction.RecentSelected(query)) },
                                enabled = true,
                                variant = StreamCoreTvButtonVariant.Secondary,
                                modifier = Modifier
                                    .width(StreamCoreDimens.Tv.Search.RecentItemWidth)
                                    .testTag(SearchTestTags.recent(query)),
                            )
                        }
                    }
                }
            }
        }
        if (trending.isNotEmpty()) {
            item(key = "trending", contentType = "trending") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                    modifier = Modifier.testTag(SearchTestTags.TrendingList),
                ) {
                    Text(
                        text = stringResource(R.string.search_trending_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val rowState = rememberLazyListState()
                    val focusRequester = remember { FocusRequester() }
                    val selectedIndex = remember(trending, returnFocusKey) {
                        trending.indexOfContentKey(returnFocusKey)
                    }
                    LaunchedEffect(selectedIndex, returnFocusKey) {
                        val focusKey = returnFocusKey ?: return@LaunchedEffect
                        val targetRequester = if (selectedIndex >= 0) {
                            rowState.scrollToItem(selectedIndex)
                            focusRequester
                        } else {
                            fallbackFocusRequester
                        }
                        if (targetRequester.requestFocusWhenReady()) {
                            onReturnFocusConsumed(focusKey)
                        }
                    }
                    LazyRow(
                        state = rowState,
                        modifier = Modifier.focusRestorer(),
                        horizontalArrangement = Arrangement.spacedBy(
                            StreamCoreDimens.Spacing.Large,
                        ),
                        contentPadding = PaddingValues(
                            vertical = StreamCoreDimens.Tv.Focus.BorderPadding,
                        ),
                    ) {
                        items(
                            items = trending,
                            key = { content -> content.id },
                            contentType = { "trending-content" },
                        ) { content ->
                            StreamCoreTvContentCard(
                                content = content,
                                type = RowType.Landscape,
                                onClick = {
                                    onAction(SearchAction.TrendingSelected(content))
                                },
                                selectedContentKey = selectedContentKey,
                                sharedElementScope = sharedElementScope,
                                focusRequester = if (content.matchesContentKey(returnFocusKey)) {
                                    focusRequester
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchResults(
    items: List<ContentModel>,
    showOfflineNotice: Boolean,
    onSelected: (ContentModel) -> Unit,
    selectedContentKey: String?,
    returnFocusKey: String?,
    fallbackFocusRequester: FocusRequester,
    onReturnFocusConsumed: (String) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    val selectedIndex = remember(items, returnFocusKey) {
        items.indexOfContentKey(returnFocusKey)
    }

    LaunchedEffect(selectedIndex, returnFocusKey) {
        val focusKey = returnFocusKey ?: return@LaunchedEffect
        val targetRequester = if (selectedIndex >= 0) {
            gridState.scrollToItem(selectedIndex)
            focusRequester
        } else {
            fallbackFocusRequester
        }
        if (targetRequester.requestFocusWhenReady()) {
            onReturnFocusConsumed(focusKey)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_results_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (showOfflineNotice) {
                Text(
                    text = stringResource(R.string.search_offline_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(StreamCoreDimens.Tv.Browse.PosterCardWidth),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            contentPadding = PaddingValues(
                top = StreamCoreDimens.Tv.Focus.BorderPadding,
                bottom = StreamCoreDimens.Tv.Screen.VerticalPadding,
            ),
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer()
                .testTag(SearchTestTags.ResultsGrid),
        ) {
            items(
                items = items,
                key = { content -> "${content.row}:${content.id}" },
                contentType = { "search-result" },
            ) { content ->
                StreamCoreTvContentCard(
                    content = content,
                    type = RowType.Poster,
                    onClick = { onSelected(content) },
                    selectedContentKey = selectedContentKey,
                    sharedElementScope = sharedElementScope,
                    focusRequester = if (content.matchesContentKey(returnFocusKey)) {
                        focusRequester
                    } else {
                        null
                    },
                    modifier = Modifier.testTag(SearchTestTags.result(content.id)),
                )
            }
        }
    }
}

@Composable
private fun TvSearchLoading() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.Loading),
    ) {
        Box(
            modifier = Modifier
                .width(StreamCoreDimens.Tv.Loading.TitleWidth)
                .height(StreamCoreDimens.Tv.Loading.TitleHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(StreamCoreDimens.Tv.Browse.PosterCardWidth)
                        .height(
                            StreamCoreDimens.Tv.Browse.PosterCardWidth /
                                    StreamCoreDimens.Artwork.PosterAspectRatio,
                        )
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

@Composable
private fun TvSearchStatus(text: String) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TvSearchMessage(
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
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StreamCoreTvButton(
                text = action,
                onClick = onAction,
                enabled = true,
                variant = StreamCoreTvButtonVariant.Primary,
            )
        }
    }
}

private fun List<ContentModel>.indexOfContentKey(selectedContentKey: String?): Int {
    if (selectedContentKey == null) {
        return -1
    }
    return indexOfFirst { content -> content.matchesContentKey(selectedContentKey) }
}

private fun ContentModel.matchesContentKey(selectedContentKey: String?): Boolean {
    return selectedContentKey != null && StreamCoreSharedKey.content(
        contentId = id,
        row = row,
    ) == selectedContentKey
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

@PreviewTV
@Composable
private fun TvSearchScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvSearchScreen(
            state = SearchPreviewData.discovery,
            onAction = {},
            onKeyboardRequested = {},
            fieldFocusRequester = remember { FocusRequester() },
        )
    }
}

@PreviewTV
@Composable
private fun TvSearchResultsPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvSearchScreen(
            state = SearchPreviewData.results,
            onAction = {},
            onKeyboardRequested = {},
            fieldFocusRequester = remember { FocusRequester() },
        )
    }
}

private const val ContentCrossfadeMillis = 200
private const val FocusRequestAttempts = 3
