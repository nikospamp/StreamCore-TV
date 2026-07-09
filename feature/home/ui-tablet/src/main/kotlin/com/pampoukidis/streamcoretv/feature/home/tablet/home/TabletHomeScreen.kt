package com.pampoukidis.streamcoretv.feature.home.tablet.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBrowseTopBar
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButtonSize
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreInfoIcon
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreArtworkSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreContentSharedIdentity
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreTitleSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.home.toHomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val content = remember(state.rows) {
        state.rows.toHomeContentModel()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.Root),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            StreamCoreBrowseTopBar(
                onProfileSelected = onProfileSelected,
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Browse.Tablet.ScreenHorizontalPadding,
                ),
            )
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.rows.isNotEmpty(),
                onRefresh = { onAction(HomeAction.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HomeTestTags.PullToRefresh),
            ) {
                TabletHomeContent(
                    state = state,
                    content = content,
                    onContentSelected = { selected ->
                        onAction(HomeAction.ContentSelected(selected))
                    },
                    selectedContentKey = selectedContentKey,
                    sharedElementScope = sharedElementScope,
                )
            }
        }
    }
}

@Composable
private fun TabletHomeContent(
    state: HomeUiState,
    content: HomeContentModel,
    onContentSelected: (ContentModel) -> Unit,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.rows.isEmpty() -> CircularProgressIndicator()
            content.featured.isEmpty() -> Text(
                text = "No content available.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.Xl),
                verticalArrangement = Arrangement.spacedBy(
                    StreamCoreDimens.Browse.Tablet.SectionSpacing,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(
                    key = HomeTestTags.Hero,
                    contentType = "hero",
                ) {
                    TabletHeroArea(
                        featured = content.featured,
                        continueWatching = content.continueWatching,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onContentSelected = onContentSelected,
                    )
                }
                items(
                    items = content.shelves,
                    key = { row -> row.id },
                    contentType = { row -> row.type },
                ) { row ->
                    TabletShelf(
                        row = row,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onContentSelected = onContentSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletHeroArea(
    featured: List<ContentModel>,
    continueWatching: RowModel?,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Md),
        modifier = Modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Browse.Tablet.HeroHeight)
            .padding(
                horizontal = StreamCoreDimens.Browse.Tablet.ScreenHorizontalPadding,
            ),
    ) {
        TabletHeroPager(
            content = featured,
            selectedContentKey = selectedContentKey,
            sharedElementScope = sharedElementScope,
            onContentSelected = onContentSelected,
            modifier = Modifier.weight(1f),
        )
        continueWatching?.let { row ->
            ContinueWatchingPanel(
                row = row,
                selectedContentKey = selectedContentKey,
                sharedElementScope = sharedElementScope,
                onContentSelected = onContentSelected,
                modifier = Modifier.width(
                    StreamCoreDimens.Browse.Tablet.BookmarkPanelWidth,
                ),
            )
        }
    }
}

@Composable
private fun TabletHeroPager(
    content: List<ContentModel>,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { content.size })

    HorizontalPager(
        state = pagerState,
        pageSpacing = StreamCoreDimens.Browse.Tablet.RowSpacing,
        modifier = modifier.fillMaxHeight(),
    ) { page ->
        val item = content[page]
        TabletHeroCard(
            content = item,
            pageCount = content.size,
            selectedPage = pagerState.currentPage,
            selectedContentKey = selectedContentKey,
            sharedElementScope = sharedElementScope,
            onClick = { onContentSelected(item) },
        )
    }
}

@Composable
private fun TabletHeroCard(
    content: ContentModel,
    pageCount: Int,
    selectedPage: Int,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val useSharedTransition = content.sharedIdentity() == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val heroShape = MaterialTheme.shapes.extraLarge

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
                    key = streamCoreArtworkSharedKey(
                        contentId = content.id,
                        row = content.row,
                    ),
                    clipShape = heroShape,
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Xs),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = StreamCoreDimens.Spacing.Xl, vertical = 22.dp),
        ) {
            Text(
                text = "Featured movie",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.streamCoreSharedBounds(
                    sharedElementScope = elementScope,
                    key = streamCoreTitleSharedKey(
                        contentId = content.id,
                        row = content.row,
                    ),
                    clipShape = RectangleShape,
                ),
            )
            Text(
                text = content.heroMetadata(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            StreamCoreButton(
                text = "Details",
                onClick = onClick,
                enabled = true,
                size = StreamCoreButtonSize.Compact,
                leadingIcon = { StreamCoreInfoIcon() },
                modifier = Modifier.testTag(HomeTestTags.HeroDetails),
            )
            PagerIndicator(
                pageCount = pageCount,
                selectedPage = selectedPage,
            )
        }
    }
}

@Composable
private fun ContinueWatchingPanel(
    row: RowModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxHeight()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Sm),
            modifier = Modifier.padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            row.content.take(3).forEach { item ->
                ContinueWatchingItem(
                    rowId = row.id,
                    content = item,
                    selectedContentKey = selectedContentKey,
                    sharedElementScope = sharedElementScope,
                    onClick = { onContentSelected(item) },
                )
            }
            Text(
                text = "Bookmarks stay one tap away while the main area keeps browsing momentum.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = StreamCoreDimens.Spacing.Xxs),
            )
        }
    }
}

@Composable
private fun ContinueWatchingItem(
    rowId: String,
    content: ContentModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val useSharedTransition = content.sharedIdentity() == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val progress = content.playbackProgress?.fraction ?: 0f
    val shape = MaterialTheme.shapes.small

    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(56.dp)
                .clip(shape),
        ) {
            StreamCoreContentImage(
                imageUrl = content.backdrop ?: content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = streamCoreArtworkSharedKey(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = shape,
                    ),
            )
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.24f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Xxs),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.homeMetadataText(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TabletShelf(
    row: RowModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Browse.Tablet.RowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        ShelfHeader(
            title = row.title,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Browse.Tablet.ScreenHorizontalPadding,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Browse.Tablet.ScreenHorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                StreamCoreDimens.Browse.Tablet.RowSpacing,
            ),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.type },
            ) { index, item ->
                when (row.type) {
                    RowType.Poster -> TabletArtworkCard(
                        rowId = row.id,
                        content = item,
                        width = StreamCoreDimens.Browse.Tablet.PosterWidth,
                        height = StreamCoreDimens.Browse.Tablet.PosterHeight,
                        imageUrl = item.poster,
                        showMetadata = true,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item) },
                    )

                    RowType.TopTen -> TabletTopTenCard(
                        rowId = row.id,
                        content = item,
                        rank = index + 1,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item) },
                    )

                    RowType.Featured,
                    RowType.ContinueWatching,
                    RowType.Landscape -> TabletArtworkCard(
                        rowId = row.id,
                        content = item,
                        width = StreamCoreDimens.Browse.Tablet.LandscapeWidth,
                        height = StreamCoreDimens.Browse.Tablet.LandscapeHeight,
                        imageUrl = item.backdrop ?: item.poster,
                        showMetadata = false,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TabletArtworkCard(
    rowId: String,
    content: ContentModel,
    width: Dp,
    height: Dp,
    imageUrl: String?,
    showMetadata: Boolean,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val useSharedTransition = content.sharedIdentity() == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val shape = MaterialTheme.shapes.medium

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape),
        ) {
            StreamCoreContentImage(
                imageUrl = imageUrl,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = streamCoreArtworkSharedKey(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = shape,
                    ),
            )
            CardGradient()
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(9.dp)
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = streamCoreTitleSharedKey(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = RectangleShape,
                    ),
            )
        }
        if (showMetadata) {
            Text(
                text = content.homeMetadataText(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TabletTopTenCard(
    rowId: String,
    content: ContentModel,
    rank: Int,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val useSharedTransition = content.sharedIdentity() == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val posterShape = MaterialTheme.shapes.medium

    Box(
        modifier = Modifier
            .width(StreamCoreDimens.Browse.Tablet.TopTenWidth)
            .height(StreamCoreDimens.Browse.Tablet.TopTenHeight)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Text(
            text = rank.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
            fontSize = 92.sp,
            lineHeight = 82.sp,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.displayLarge.copy(
                drawStyle = Stroke(width = 1.4f),
            ),
            modifier = Modifier.align(Alignment.BottomStart),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(StreamCoreDimens.Browse.Tablet.TopTenPosterWidth)
                .height(StreamCoreDimens.Browse.Tablet.TopTenPosterHeight)
                .clip(posterShape),
        ) {
            StreamCoreContentImage(
                imageUrl = content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = streamCoreArtworkSharedKey(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = posterShape,
                    ),
            )
            CardGradient()
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = streamCoreTitleSharedKey(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = RectangleShape,
                    ),
            )
        }
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    selectedPage: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .width(if (index == selectedPage) 18.dp else 5.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedPage) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.32f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun CardGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.76f),
                    ),
                ),
            ),
    )
}

private fun ContentModel.sharedIdentity(): String {
    return streamCoreContentSharedIdentity(
        contentId = id,
        row = row,
    )
}

private fun ContentModel.heroMetadata(): String {
    val values = buildList {
        val year = releaseYear(releaseDate)
        if (year > 0) {
            add(year.toString())
        }
        genres.firstOrNull()?.name?.let(::add)
        add(homeMetadataText())
    }
    return values.joinToString(separator = "  ·  ")
}

private fun releaseYear(epochMillis: Long): Int {
    if (epochMillis <= 0L) {
        return 0
    }

    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        timeInMillis = epochMillis
        get(Calendar.YEAR)
    }
}

@PreviewTablet
@Composable
private fun TabletHomeScreenPreview() {
    StreamCoreTVTheme(darkTheme = true) {
        TabletHomeScreen(
            state = HomeUiState(
                isLoading = false,
                rows = HomePreviewData.rows,
            ),
            onAction = {},
            onProfileSelected = {},
            selectedContentKey = null,
        )
    }
}
