package com.pampoukidis.streamcoretv.feature.home.tv.home

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreInfoIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvCarousel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvContentCard
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreOverlayDuringSharedTransition
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.home.toHomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags

@Composable
fun TvHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    returnFocusKey: String? = null,
    onReturnFocusConsumed: (String) -> Unit = {},
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val content = remember(state.rows) { state.rows.toHomeContentModel() }
    val refreshFocusRequester = remember { FocusRequester() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = StreamCoreDimens.Tv.Screen.VerticalPadding),
        ) {
            HomeHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                focusRequester = refreshFocusRequester,
            )
            if (state.isLoading && state.rows.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                            end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                        ),
                )
            }
            TvHomeBody(
                state = state,
                content = content,
                onAction = onAction,
                refreshFocusRequester = refreshFocusRequester,
                selectedContentKey = selectedContentKey,
                returnFocusKey = returnFocusKey,
                onReturnFocusConsumed = onReturnFocusConsumed,
                sharedElementScope = sharedElementScope,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    isLoading: Boolean,
    onAction: (HomeAction) -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            ),
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        StreamCoreTvButton(
            text = "Refresh",
            onClick = { onAction(HomeAction.Refresh) },
            enabled = !isLoading,
            variant = StreamCoreTvButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(focusRequester)
                .testTag(HomeTestTags.RefreshButton),
        )
    }
}

@Composable
private fun TvHomeBody(
    state: HomeUiState,
    content: HomeContentModel,
    onAction: (HomeAction) -> Unit,
    refreshFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    selectedContentKey: String?,
    returnFocusKey: String?,
    onReturnFocusConsumed: (String) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    val listState = rememberLazyListState()
    val heroFocusRequester = remember { FocusRequester() }
    var hasAssignedFocus by remember { mutableStateOf(false) }
    val rows = remember(content) {
        buildList {
            content.continueWatching?.let(::add)
            addAll(content.shelves)
        }
    }
    val selectedHeroIndex = remember(content.featured, returnFocusKey) {
        content.featured.indexOfContentKey(returnFocusKey)
    }
    val rowFocusLocation = remember(rows, returnFocusKey, content.featured) {
        rows.findFocusLocation(
            selectedContentKey = returnFocusKey,
            useFallback = content.featured.isEmpty(),
        )
    }
    val heroItemCount = if (content.featured.isEmpty()) 0 else 1

    LaunchedEffect(rowFocusLocation?.rowIndex, heroItemCount) {
        rowFocusLocation?.let { location ->
            listState.scrollToItem(location.rowIndex + heroItemCount)
        }
    }

    LaunchedEffect(
        content.featured,
        selectedHeroIndex,
        rowFocusLocation,
        state.isLoading,
        returnFocusKey,
        hasAssignedFocus,
    ) {
        if (state.isLoading || hasAssignedFocus) {
            return@LaunchedEffect
        }
        val focused = when {
            content.featured.isNotEmpty() && rowFocusLocation == null -> {
                if (returnFocusKey == null) {
                    withFrameNanos { }
                    heroFocusRequester.requestFocus()
                } else {
                    heroFocusRequester.requestFocusWhenReady()
                }
            }

            content.featured.isEmpty() && rows.isEmpty() -> {
                if (returnFocusKey == null) {
                    withFrameNanos { }
                    refreshFocusRequester.requestFocus()
                } else {
                    refreshFocusRequester.requestFocusWhenReady()
                }
            }

            else -> false
        }
        if (focused) {
            returnFocusKey?.let { focusKey ->
                hasAssignedFocus = true
                onReturnFocusConsumed(focusKey)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.rows.isEmpty() -> TvHomeLoadingContent()
            content.featured.isEmpty() && rows.isEmpty() -> Text(
                text = "No content available.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    bottom = StreamCoreDimens.Tv.Screen.VerticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(
                    StreamCoreDimens.Spacing.ExtraLarge,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRestorer(),
            ) {
                if (content.featured.isNotEmpty()) {
                    item(
                        key = HomeTestTags.Hero,
                        contentType = "tv-hero",
                    ) {
                        TvHomeHeroCarousel(
                            content = content.featured,
                            initialActiveItemIndex = selectedHeroIndex.coerceAtLeast(0),
                            focusRequester = heroFocusRequester,
                            selectedContentKey = selectedContentKey,
                            sharedElementScope = sharedElementScope,
                            onContentSelected = { selected ->
                                onAction(HomeAction.ContentSelected(selected))
                            },
                        )
                    }
                }

                itemsIndexed(
                    items = rows,
                    key = { _, row -> row.id },
                    contentType = { _, row -> row.type },
                ) { rowIndex, row ->
                    TvContentRow(
                        row = row,
                        onAction = onAction,
                        focusContentIndex = if (rowIndex == rowFocusLocation?.rowIndex) {
                            rowFocusLocation.contentIndex
                        } else {
                            null
                        },
                        selectedContentKey = selectedContentKey,
                        returnFocusKey = returnFocusKey,
                        focusRequestEnabled = !hasAssignedFocus,
                        onFocusAssigned = { consumedKey ->
                            consumedKey?.let { focusKey ->
                                hasAssignedFocus = true
                                onReturnFocusConsumed(focusKey)
                            }
                        },
                        sharedElementScope = sharedElementScope,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvHomeHeroCarousel(
    content: List<ContentModel>,
    initialActiveItemIndex: Int,
    focusRequester: FocusRequester,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
) {
    StreamCoreTvCarousel(
        itemCount = content.size,
        initialActiveItemIndex = initialActiveItemIndex,
        modifier = Modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Tv.Browse.HeroHeight)
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            )
            .testTag(HomeTestTags.Hero),
    ) { index ->
        val item = content[index]
        TvHomeHero(
            content = item,
            focusRequester = focusRequester,
            useSharedTransition = item.sharedContentKey() == selectedContentKey,
            sharedElementScope = sharedElementScope,
            onClick = { onContentSelected(item) },
        )
    }
}

@Composable
private fun TvHomeHero(
    content: ContentModel,
    focusRequester: FocusRequester,
    useSharedTransition: Boolean,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val heroShape = MaterialTheme.shapes.extraLarge
    val elementScope = sharedElementScope.takeIf { useSharedTransition }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(heroShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        StreamCoreContentImage(
            imageUrl = content.backdrop ?: content.poster,
            contentDescription = content.title,
            fallbackText = content.fallbackText(),
            contentScale = ContentScale.Crop,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fallbackTextStyle = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .fillMaxSize()
                .streamCoreSharedBounds(
                    sharedElementScope = elementScope,
                    key = StreamCoreSharedKey.artwork(
                        contentId = content.id,
                        row = content.row,
                    ),
                    clipShape = heroShape,
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = elementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Scrim,
                    clipShape = heroShape,
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f),
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f),
                        ),
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.56f)
                .streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = elementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                )
                .padding(StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            Text(
                text = "Featured movie",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onArtwork,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.streamCoreSharedBounds(
                    sharedElementScope = elementScope,
                    key = StreamCoreSharedKey.title(
                        contentId = content.id,
                        row = content.row,
                    ),
                    clipShape = RectangleShape,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                ),
            )
            Text(
                text = content.heroMetadata(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            StreamCoreTvButton(
                text = "Details",
                onClick = onClick,
                enabled = true,
                variant = StreamCoreTvButtonVariant.Primary,
                leadingIcon = { StreamCoreInfoIcon() },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .testTag(HomeTestTags.HeroDetails),
            )
        }
    }
}

@Composable
private fun TvContentRow(
    row: RowModel,
    onAction: (HomeAction) -> Unit,
    focusContentIndex: Int?,
    modifier: Modifier = Modifier,
    selectedContentKey: String?,
    returnFocusKey: String?,
    focusRequestEnabled: Boolean,
    onFocusAssigned: (String?) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    val rowState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusContentIndex, returnFocusKey, focusRequestEnabled) {
        if (!focusRequestEnabled) {
            return@LaunchedEffect
        }
        focusContentIndex?.let { contentIndex ->
            rowState.scrollToItem(contentIndex)
            val focused = if (returnFocusKey == null) {
                withFrameNanos { }
                focusRequester.requestFocus()
            } else {
                focusRequester.requestFocusWhenReady()
            }
            if (focused) {
                onFocusAssigned(returnFocusKey)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            ),
        )
        LazyRow(
            state = rowState,
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                top = StreamCoreDimens.Tv.Focus.BorderPadding,
                bottom = StreamCoreDimens.Tv.Focus.BorderPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.type },
            ) { contentIndex, item ->
                StreamCoreTvContentCard(
                    content = item,
                    type = row.type,
                    rank = contentIndex + 1,
                    onClick = { onAction(HomeAction.ContentSelected(item)) },
                    focusRequester = if (contentIndex == focusContentIndex) {
                        focusRequester
                    } else {
                        null
                    },
                    selectedContentKey = selectedContentKey,
                    sharedElementScope = sharedElementScope,
                    modifier = Modifier.testTag(
                        HomeTestTags.ContentCardPrefix + row.id + ":" + item.id,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TvHomeLoadingContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StreamCoreDimens.Tv.Browse.HeroHeight)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Box(
            modifier = Modifier
                .width(StreamCoreDimens.Tv.Loading.TitleWidth)
                .height(StreamCoreDimens.Tv.Loading.TitleHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

private fun List<ContentModel>.indexOfContentKey(selectedContentKey: String?): Int {
    if (selectedContentKey == null) {
        return -1
    }
    return indexOfFirst { content -> content.sharedContentKey() == selectedContentKey }
}

private fun List<RowModel>.findFocusLocation(
    selectedContentKey: String?,
    useFallback: Boolean,
): TvFocusLocation? {
    if (selectedContentKey != null) {
        forEachIndexed { rowIndex, row ->
            val contentIndex = row.content.indexOfContentKey(selectedContentKey)
            if (contentIndex >= 0) {
                return TvFocusLocation(
                    rowIndex = rowIndex,
                    contentIndex = contentIndex,
                )
            }
        }
    }

    if (!useFallback) {
        return null
    }
    val firstPopulatedRowIndex = indexOfFirst { row -> row.content.isNotEmpty() }
    if (firstPopulatedRowIndex < 0) {
        return null
    }
    return TvFocusLocation(
        rowIndex = firstPopulatedRowIndex,
        contentIndex = 0,
    )
}

private fun ContentModel.sharedContentKey(): String {
    return StreamCoreSharedKey.content(
        contentId = id,
        row = row,
    )
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

private data class TvFocusLocation(
    val rowIndex: Int,
    val contentIndex: Int,
)

private const val FocusRequestAttempts = 3

@PreviewTV
@Composable
private fun TvHomeScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvHomeScreen(
            state = HomeUiState(
                isLoading = false,
                rows = HomePreviewData.rows,
            ),
            onAction = {},
        )
    }
}
