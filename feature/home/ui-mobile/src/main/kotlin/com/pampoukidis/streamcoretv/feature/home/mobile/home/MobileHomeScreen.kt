package com.pampoukidis.streamcoretv.feature.home.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
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
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.home.toHomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags

@Composable
fun MobileHomeScreen(
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
                    horizontal = StreamCoreDimens.Browse.Mobile.ScreenHorizontalPadding,
                ),
            )
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.rows.isNotEmpty(),
                onRefresh = { onAction(HomeAction.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HomeTestTags.PullToRefresh),
            ) {
                MobileHomeContent(
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
private fun MobileHomeContent(
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

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.Xl),
                verticalArrangement = Arrangement.spacedBy(
                    StreamCoreDimens.Browse.Mobile.SectionSpacing,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(
                    key = HomeTestTags.Hero,
                    contentType = "hero",
                ) {
                    MobileHeroPager(
                        content = content.featured,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onContentSelected = onContentSelected,
                    )
                }
                content.continueWatching?.let { row ->
                    item(
                        key = row.id,
                        contentType = row.type,
                    ) {
                        MobileContinueWatchingRow(
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
                    MobileShelf(
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
private fun MobileHeroPager(
    content: List<ContentModel>,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { content.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(
            horizontal = StreamCoreDimens.Browse.Mobile.ScreenHorizontalPadding,
        ),
        pageSpacing = StreamCoreDimens.Browse.Mobile.RowSpacing,
        modifier = Modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Browse.Mobile.HeroHeight),
    ) { page ->
        val item = content[page]
        HomeHeroCard(
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
private fun HomeHeroCard(
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
                            Color.Black.copy(alpha = 0.25f),
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
                .padding(StreamCoreDimens.Spacing.Md),
        ) {
            Text(
                text = "Featured movie",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.displaySmall,
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
private fun MobileContinueWatchingRow(
    row: RowModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Browse.Mobile.RowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        HomeShelfHeader(
            title = row.title,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Browse.Mobile.ScreenHorizontalPadding,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Browse.Mobile.ScreenHorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                StreamCoreDimens.Browse.Mobile.RowSpacing,
            ),
        ) {
            items(
                items = row.content,
                key = { content -> content.id },
                contentType = { "continue-watching" },
            ) { item ->
                ContinueWatchingCard(
                    rowId = row.id,
                    content = item,
                    selectedContentKey = selectedContentKey,
                    sharedElementScope = sharedElementScope,
                    onClick = { onContentSelected(item) },
                )
            }
        }
    }
}

@Composable
private fun MobileShelf(
    row: RowModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onContentSelected: (ContentModel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Browse.Mobile.RowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        HomeShelfHeader(
            title = row.title,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Browse.Mobile.ScreenHorizontalPadding,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = if (row.type == RowType.TopTen) {
                    15.dp
                } else {
                    StreamCoreDimens.Browse.Mobile.ScreenHorizontalPadding
                },
            ),
            horizontalArrangement = Arrangement.spacedBy(
                if (row.type == RowType.TopTen) {
                    16.dp
                } else {
                    StreamCoreDimens.Browse.Mobile.RowSpacing
                },
            ),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.type },
            ) { index, item ->
                when (row.type) {
                    RowType.Poster -> PosterCard(
                        rowId = row.id,
                        content = item,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item) },
                    )

                    RowType.TopTen -> TopTenCard(
                        rowId = row.id,
                        content = item,
                        rank = index + 1,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onClick = { onContentSelected(item) },
                    )

                    RowType.Featured,
                    RowType.ContinueWatching,
                    RowType.Landscape -> LandscapeCard(
                        rowId = row.id,
                        content = item,
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
private fun HomeShelfHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ContinueWatchingCard(
    rowId: String,
    content: ContentModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    val useSharedTransition = content.sharedIdentity() == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val progress = content.playbackProgress?.fraction ?: 0f
    val shape = MaterialTheme.shapes.medium

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(164.dp)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
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
            CardGradient()
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 9.dp, end = 9.dp, bottom = 12.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(horizontal = 9.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PosterCard(
    rowId: String,
    content: ContentModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    FlatArtworkCard(
        rowId = rowId,
        content = content,
        width = StreamCoreDimens.Browse.Mobile.PosterWidth,
        height = StreamCoreDimens.Browse.Mobile.PosterHeight,
        imageUrl = content.poster,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
        onClick = onClick,
    )
}

@Composable
private fun LandscapeCard(
    rowId: String,
    content: ContentModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
) {
    FlatArtworkCard(
        rowId = rowId,
        content = content,
        width = StreamCoreDimens.Browse.Mobile.LandscapeWidth,
        height = StreamCoreDimens.Browse.Mobile.LandscapeHeight,
        imageUrl = content.backdrop ?: content.poster,
        selectedContentKey = selectedContentKey,
        sharedElementScope = sharedElementScope,
        onClick = onClick,
    )
}

@Composable
private fun FlatArtworkCard(
    rowId: String,
    content: ContentModel,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    imageUrl: String?,
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
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopTenCard(
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
            .width(StreamCoreDimens.Browse.Mobile.TopTenWidth)
            .height(StreamCoreDimens.Browse.Mobile.TopTenHeight)
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(StreamCoreDimens.Browse.Mobile.TopTenPosterWidth)
                .height(StreamCoreDimens.Browse.Mobile.TopTenPosterHeight)
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
                    .padding(8.dp)
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
        Text(
            text = rank.toString(),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 118.sp,
            lineHeight = 96.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomStart),
        )

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

@PreviewMobile
@Composable
private fun MobileHomeScreenPreview() {
    StreamCoreTVTheme(darkTheme = true) {
        MobileHomeScreen(
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

@PreviewMobile
@Composable
private fun MobileHomeScreenLightPreview() {
    StreamCoreTVTheme(darkTheme = false) {
        MobileHomeScreen(
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