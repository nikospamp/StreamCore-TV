package com.pampoukidis.streamcoretv.feature.home.tablet.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBrowseTopBar
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButtonSize
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSharedArtworkImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreInfoIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePagerCarousel
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreOverlayDuringSharedTransition
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTextStyles
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
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
    activeProfile: ProfileModel? = null,
    bottomContentPadding: Dp = StreamCoreDimens.Spacing.ExtraLarge,
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                    onContentSelected = { selected, sourceArtworkUrl ->
                        onAction(
                            HomeAction.ContentSelected(
                                content = selected,
                                sourceArtworkUrl = sourceArtworkUrl,
                            ),
                        )
                    },
                    selectedContentKey = selectedContentKey,
                    sharedElementScope = sharedElementScope,
                    bottomContentPadding = bottomContentPadding,
                )
            }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onArtwork) {
                StreamCoreBrowseTopBar(
                    onProfileSelected = onProfileSelected,
                    profileAvatar = activeProfile?.avatar,
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f),
                                0.85f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
                )
            }
        }
    }
}

@Composable
private fun TabletHomeContent(
    state: HomeUiState,
    content: HomeContentModel,
    onContentSelected: (ContentModel, String?) -> Unit,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    bottomContentPadding: Dp,
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
                contentPadding = PaddingValues(bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(
                    StreamCoreDimens.Tablet.Browse.SectionSpacing,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(
                    key = HomeTestTags.Hero,
                    contentType = "hero",
                ) {
                    TabletHeroArea(
                        featured = content.featured,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onContentSelected = onContentSelected,
                    )
                }
                content.continueWatching?.let { row ->
                    item(key = row.id, contentType = row.type) {
                        ContinueWatchingShelf(
                            row = row,
                            selectedContentKey = selectedContentKey,
                            sharedElementScope = sharedElementScope,
                            onContentSelected = onContentSelected,
                        )
                    }
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
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel, String?) -> Unit,
) {
    TabletHeroPager(
        content = featured,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
        onContentSelected = onContentSelected,
        modifier = Modifier.fillMaxWidth().height(StreamCoreDimens.Tablet.Browse.HeroHeight),
    )
}

@Composable
private fun TabletHeroPager(
    content: List<ContentModel>,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Count and retained pager callbacks share one list snapshot during catalogue updates.
    val carouselItems by rememberUpdatedState(content)
    val pagerState = rememberPagerState(pageCount = { carouselItems.size })

    StreamCorePagerCarousel(
        state = pagerState,
        key = { page -> carouselItems[page].sharedIdentity() },
        indicatorPadding = PaddingValues(StreamCoreDimens.Spacing.ExtraLarge),
        pageSpacing = StreamCoreDimens.Tablet.Browse.RowSpacing,
        modifier = modifier.fillMaxHeight().testTag(HomeTestTags.Hero),
    ) { page ->
        val item = carouselItems[page]
        TabletHeroCard(
            content = item,
            selectedContentKey = selectedContentKey,
            sharedElementScope = sharedElementScope,
            onClick = { onContentSelected(item, item.backdrop ?: item.poster) },
        )
    }
}

@Composable
private fun TabletHeroCard(
    content: ContentModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val useSharedTransition = content.sharedIdentity() == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val heroShape = RectangleShape
    val scrimColor = MaterialTheme.colorScheme.scrim
    val backgroundColor = MaterialTheme.colorScheme.background
    val artworkScrims = remember(scrimColor, backgroundColor) {
        listOf(
            Brush.horizontalGradient(
                colors = listOf(
                    scrimColor.copy(alpha = 0.9f),
                    scrimColor.copy(alpha = 0.22f),
                    scrimColor.copy(alpha = 0f),
                ),
            ),
            Brush.verticalGradient(
                0f to scrimColor.copy(alpha = 0f),
                0.5f to scrimColor.copy(alpha = 0f),
                1f to backgroundColor,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        StreamCoreSharedArtworkImage(
            imageUrl = content.backdrop ?: content.poster,
            contentDescription = content.title,
            fallbackText = content.fallbackText(),
            sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
            clipShape = heroShape,
            sharedElementScope = elementScope,
            scrims = artworkScrims,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fallbackTextStyle = MaterialTheme.typography.displayLarge,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = StreamCoreDimens.Tablet.Browse.HeroCopyMaxWidth)
                .fillMaxWidth()
                .streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = elementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                )
                .padding(
                    start = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                    end = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                    top = StreamCoreDimens.Spacing.ExtraLarge,
                    bottom = StreamCoreDimens.Spacing.ExtraLarge + StreamCoreDimens.Indicator.DotSize +
                        StreamCoreDimens.Spacing.Small,
                ),
        ) {
            Text(
                text = "Featured movie",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onArtwork,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.heroMetadata(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.8f),
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
        }
    }
}

@Composable
private fun ContinueWatchingShelf(
    row: RowModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel, String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.RowSpacing),
        modifier = Modifier.fillMaxWidth().testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        ShelfHeader(
            title = row.title,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.RowSpacing),
            contentPadding = PaddingValues(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
        ) {
            items(items = row.content, key = { it.id }, contentType = { row.type }) { item ->
                Box(modifier = Modifier.width(StreamCoreDimens.Tablet.Browse.LandscapeWidth)) {
                    ContinueWatchingItem(
                        rowId = row.id,
                        content = item,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item, item.backdrop ?: item.poster) },
                    )
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .width(StreamCoreDimens.Tablet.Browse.BookmarkThumbnailWidth)
                .height(StreamCoreDimens.Tablet.Browse.BookmarkThumbnailHeight)
                .clip(shape),
        ) {
            StreamCoreSharedArtworkImage(
                imageUrl = content.backdrop ?: content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
                clipShape = shape,
                sharedElementScope = elementScope,
                modifier = Modifier.fillMaxSize(),
            )
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.2f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(StreamCoreDimens.Artwork.ProgressHeight)
                    .streamCoreOverlayDuringSharedTransition(
                        sharedElementScope = elementScope,
                        zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                    ),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
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
    onContentSelected: (ContentModel, String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.RowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        ShelfHeader(
            title = row.title,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                StreamCoreDimens.Tablet.Browse.RowSpacing,
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
                        width = StreamCoreDimens.Tablet.Browse.PosterWidth,
                        aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
                        imageUrl = item.poster,
                        showMetadata = true,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item, item.poster) },
                    )

                    RowType.TopTen -> TabletTopTenCard(
                        rowId = row.id,
                        content = item,
                        rank = index + 1,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item, item.poster) },
                    )

                    RowType.Featured,
                    RowType.ContinueWatching,
                    RowType.Landscape -> TabletArtworkCard(
                        rowId = row.id,
                        content = item,
                        width = StreamCoreDimens.Tablet.Browse.LandscapeWidth,
                        aspectRatio = StreamCoreDimens.Artwork.LandscapeAspectRatio,
                        imageUrl = item.backdrop ?: item.poster,
                        showMetadata = false,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item, item.backdrop ?: item.poster) },
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
    aspectRatio: Float,
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
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(shape),
        ) {
            StreamCoreSharedArtworkImage(
                imageUrl = imageUrl,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
                clipShape = shape,
                sharedElementScope = elementScope,
                scrims = rememberCardScrims(),
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(StreamCoreDimens.Artwork.ContentPadding)
                    .streamCoreOverlayDuringSharedTransition(
                        sharedElementScope = elementScope,
                        zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
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
            .width(StreamCoreDimens.Tablet.Browse.TopTenWidth)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(StreamCoreDimens.Tablet.Browse.TopTenPosterWidth)
                .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                .clip(posterShape),
        ) {
            StreamCoreSharedArtworkImage(
                imageUrl = content.poster,
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
                clipShape = posterShape,
                sharedElementScope = elementScope,
                scrims = rememberCardScrims(),
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(StreamCoreDimens.Artwork.ContentPadding)
                    .streamCoreOverlayDuringSharedTransition(
                        sharedElementScope = elementScope,
                        zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                    ),
            )
        }
        TopTenRank(
            rank = rank,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = elementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Foreground,
                ),
        )
    }
}

@Composable
private fun TopTenRank(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = rank.toString(),
            color = MaterialTheme.colorScheme.background.copy(alpha = TopTenRankHaloAlpha),
            style = StreamCoreTextStyles.TabletTopTenRank.copy(
                drawStyle = Stroke(width = TopTenRankHaloStrokeWidth),
            ),
        )
        Text(
            text = rank.toString(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = TopTenRankContentAlpha),
            style = StreamCoreTextStyles.TabletTopTenRank,
        )
    }
}

@Composable
private fun rememberCardScrims(): List<Brush> {
    val scrimColor = MaterialTheme.colorScheme.scrim
    return remember(scrimColor) {
        listOf(
            Brush.verticalGradient(
                colors = listOf(scrimColor.copy(alpha = 0f), scrimColor.copy(alpha = 0.76f)),
            ),
        )
    }
}

private fun ContentModel.sharedIdentity(): String {
    return StreamCoreSharedKey.content(
        contentId = id,
        row = row,
    )
}

private const val TopTenRankContentAlpha = 0.76f
private const val TopTenRankHaloAlpha = 0.94f
private const val TopTenRankHaloStrokeWidth = 4.0f

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
    StreamCoreTheme(darkTheme = true) {
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
