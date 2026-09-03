package com.pampoukidis.streamcoretv.feature.home.web.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.extensions.transparentContainer
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtwork
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebContentCard
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.home.toHomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import com.pampoukidis.streamcoretv.feature.home.web.testing.WebHomePreviewData
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.home.ui_web.generated.resources.Res
import streamcoretv.feature.home.ui_web.generated.resources.web_home_empty_message
import streamcoretv.feature.home.ui_web.generated.resources.web_home_empty_title
import streamcoretv.feature.home.ui_web.generated.resources.web_home_error_message
import streamcoretv.feature.home.ui_web.generated.resources.web_home_error_title
import streamcoretv.feature.home.ui_web.generated.resources.web_home_feature_position
import streamcoretv.feature.home.ui_web.generated.resources.web_home_loading_description
import streamcoretv.feature.home.ui_web.generated.resources.web_home_more_details
import streamcoretv.feature.home.ui_web.generated.resources.web_home_next_feature
import streamcoretv.feature.home.ui_web.generated.resources.web_home_offline_message
import streamcoretv.feature.home.ui_web.generated.resources.web_home_offline_title
import streamcoretv.feature.home.ui_web.generated.resources.web_home_open_details
import streamcoretv.feature.home.ui_web.generated.resources.web_home_previous_feature
import streamcoretv.feature.home.ui_web.generated.resources.web_home_refresh
import streamcoretv.feature.home.ui_web.generated.resources.web_home_title

@Composable
fun WebHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = remember(state.rows) { state.rows.toHomeContentModel() }
    val rows = remember(content) {
        buildList {
            content.continueWatching?.let(::add)
            addAll(content.shelves)
        }
    }
    val focusTarget = remember(content, rows, returnFocusKey) {
        resolveWebHomeFocusTarget(
            content = content,
            rows = rows,
            key = returnFocusKey,
        )
    }
    val listState = rememberLazyListState()
    val refreshFocusRequester = remember { FocusRequester() }
    val emptyFocusRequester = remember { FocusRequester() }
    val currentOnReturnFocusConsumed = rememberUpdatedState(onReturnFocusConsumed)
    var consumedReturnKey by remember { mutableStateOf<WebBrowseFocusKey?>(null) }
    val consumeReturnFocus = remember(returnFocusKey) {
        { key: WebBrowseFocusKey ->
            if (key == returnFocusKey && consumedReturnKey != key) {
                consumedReturnKey = key
                currentOnReturnFocusConsumed.value(key)
            }
        }
    }

    LaunchedEffect(returnFocusKey) {
        if (returnFocusKey == null) {
            consumedReturnKey = null
        }
    }

    LaunchedEffect(focusTarget?.rowIndex, content.featured.isNotEmpty()) {
        val rowIndex = focusTarget?.rowIndex ?: return@LaunchedEffect
        val heroOffset = if (content.featured.isNotEmpty()) 1 else 0
        listState.scrollToItem(rowIndex + heroOffset)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier.fillMaxSize(),
        ) {
            WebHomeHeader(
                isLoading = state.isLoading,
                refreshFocusRequester = refreshFocusRequester,
                onRefresh = { onAction(HomeAction.Refresh) },
            )
            if (state.isLoading && state.rows.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when {
                state.isLoading && state.rows.isEmpty() -> WebHomeLoading()
                content.featured.isEmpty() && rows.isEmpty() -> WebHomeEmpty(
                    focusRequester = emptyFocusRequester,
                    onRefresh = { onAction(HomeAction.Refresh) },
                )
                else -> LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Web.Home.SectionSpacing),
                    contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (content.featured.isNotEmpty()) {
                        item(
                            key = HomeTestTags.Hero,
                            contentType = "web-home-hero",
                        ) {
                            WebHomeHero(
                                items = content.featured,
                                onAction = onAction,
                                selectedContentKey = selectedContentKey,
                                returnFocusKey = returnFocusKey,
                                returnContentIndex = focusTarget
                                    ?.takeIf { target -> target.rowIndex == null }
                                    ?.contentIndex,
                                requestInitialFocus = returnFocusKey == null,
                                onReturnFocusConsumed = consumeReturnFocus,
                            )
                        }
                    }
                    itemsIndexed(
                        items = rows,
                        key = { _, row -> row.id },
                        contentType = { _, row -> row.type },
                    ) { rowIndex, row ->
                        WebHomeRow(
                            row = row,
                            onAction = onAction,
                            selectedContentKey = selectedContentKey,
                            returnFocusKey = returnFocusKey,
                            returnContentIndex = focusTarget
                                ?.takeIf { target -> target.rowIndex == rowIndex }
                                ?.contentIndex,
                            requestInitialFocus = returnFocusKey == null &&
                                content.featured.isEmpty() && rowIndex == 0,
                            onReturnFocusConsumed = consumeReturnFocus,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebHomeHeader(
    isLoading: Boolean,
    refreshFocusRequester: FocusRequester,
    onRefresh: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.web_home_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        StreamCoreWebButton(
            text = stringResource(Res.string.web_home_refresh),
            onClick = onRefresh,
            enabled = !isLoading,
            variant = StreamCoreWebButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(refreshFocusRequester)
                .testTag(HomeTestTags.RefreshButton),
        )
    }
}

@Composable
private fun WebHomeHero(
    items: List<ContentModel>,
    onAction: (HomeAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    returnContentIndex: Int?,
    requestInitialFocus: Boolean,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
) {
    var activeIndex by rememberSaveable(items.map(ContentModel::id)) { mutableIntStateOf(0) }
    val detailsFocusRequester = remember { FocusRequester() }
    val safeActiveIndex = activeIndex.coerceIn(items.indices)
    val activeContent = items[safeActiveIndex]
    val activeFocusKey = activeContent.toWebHomeFocusKey()
    val openDetailsDescription = stringResource(
        Res.string.web_home_open_details,
        activeContent.title,
    )

    LaunchedEffect(items, returnContentIndex, returnFocusKey, requestInitialFocus) {
        val targetIndex = returnContentIndex?.takeIf { index -> index in items.indices }
        if (targetIndex != null) {
            activeIndex = targetIndex
        }
        if ((targetIndex != null || requestInitialFocus) && detailsFocusRequester.requestFocusWhenReady()) {
            if (targetIndex != null && returnFocusKey != null) {
                onReturnFocusConsumed(returnFocusKey)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Web.Home.HeroHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .testTag(HomeTestTags.Hero),
    ) {
        StreamCoreWebArtwork(
            imageUrl = activeContent.backdrop ?: activeContent.poster,
            contentDescription = activeContent.title,
            fallbackText = activeContent.fallbackText(),
            requestWidthPx = HeroRequestWidthPx,
            requestHeightPx = HeroRequestHeightPx,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.74f),
                            MaterialTheme.colorScheme.transparentContainer,
                        ),
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(StreamCoreDimens.Web.Home.HeroCopyMaxWidth)
                .padding(StreamCoreDimens.Web.Home.HeroContentPadding),
        ) {
            Text(
                text = activeContent.title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = activeContent.heroMetadata(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = activeContent.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StreamCoreWebButton(
                    text = stringResource(Res.string.web_home_more_details),
                    onClick = { onAction(HomeAction.ContentSelected(activeContent)) },
                    modifier = Modifier
                        .focusRequester(detailsFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || items.size <= 1) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    activeIndex = (safeActiveIndex - 1 + items.size) % items.size
                                    true
                                }
                                Key.DirectionRight -> {
                                    activeIndex = (safeActiveIndex + 1) % items.size
                                    true
                                }
                                else -> false
                            }
                        }
                        .semantics {
                            contentDescription = openDetailsDescription
                        }
                        .testTag(HomeTestTags.HeroDetails),
                )
                if (items.size > 1) {
                    StreamCoreWebButton(
                        text = stringResource(Res.string.web_home_previous_feature),
                        onClick = {
                            activeIndex = (safeActiveIndex - 1 + items.size) % items.size
                        },
                        variant = StreamCoreWebButtonVariant.Tertiary,
                        modifier = Modifier.testTag(WebHomeTestTags.PreviousFeature),
                    )
                    StreamCoreWebButton(
                        text = stringResource(Res.string.web_home_next_feature),
                        onClick = { activeIndex = (safeActiveIndex + 1) % items.size },
                        variant = StreamCoreWebButtonVariant.Tertiary,
                        modifier = Modifier.testTag(WebHomeTestTags.NextFeature),
                    )
                    Text(
                        text = stringResource(
                            Res.string.web_home_feature_position,
                            safeActiveIndex + 1,
                            items.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(WebHomeTestTags.HeroPosition),
                    )
                }
            }
        }
        if (selectedContentKey == activeFocusKey) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(StreamCoreDimens.Spacing.Tiny)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = SelectedOverlayAlpha)),
            )
        }
    }
}

@Composable
private fun WebHomeRow(
    row: RowModel,
    onAction: (HomeAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    returnContentIndex: Int?,
    requestInitialFocus: Boolean,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusContentIndex = returnContentIndex ?: if (requestInitialFocus) 0 else -1

    LaunchedEffect(row.id, focusContentIndex, returnFocusKey) {
        if (focusContentIndex !in row.content.indices) {
            return@LaunchedEffect
        }
        listState.scrollToItem(focusContentIndex)
        if (
            focusRequester.requestFocusWhenReady() &&
            returnContentIndex != null &&
            returnFocusKey != null
        ) {
            onReturnFocusConsumed(returnFocusKey)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.subtitle.isNotBlank()) {
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Spacing.Tiny,
                vertical = StreamCoreDimens.Spacing.Small,
            ),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.type },
            ) { contentIndex, content ->
                WebHomeContentTile(
                    content = content,
                    row = row,
                    onClick = {
                        onAction(HomeAction.ContentSelected(content.copy(row = row.id)))
                    },
                    selected = selectedContentKey?.matchesHomeContent(row.id, content.id) == true,
                    focusRequester = if (contentIndex == focusContentIndex) focusRequester else null,
                    rank = contentIndex + 1,
                )
            }
        }
    }
}

@Composable
private fun WebHomeContentTile(
    content: ContentModel,
    row: RowModel,
    onClick: () -> Unit,
    selected: Boolean,
    focusRequester: FocusRequester?,
    rank: Int,
) {
    val isPoster = row.type == RowType.Poster || row.type == RowType.TopTen
    val cardWidth = if (isPoster) {
        StreamCoreDimens.Web.Home.PosterCardWidth
    } else {
        StreamCoreDimens.Web.Home.LandscapeCardWidth
    }
    val aspectRatio = if (isPoster) {
        StreamCoreDimens.Artwork.PosterAspectRatio
    } else {
        StreamCoreDimens.Artwork.LandscapeAspectRatio
    }
    val imageUrl = if (isPoster) content.poster else content.backdrop ?: content.poster
    val requestWidth = if (isPoster) PosterRequestWidthPx else LandscapeRequestWidthPx
    val requestHeight = if (isPoster) PosterRequestHeightPx else LandscapeRequestHeightPx
    val openDetailsDescription = stringResource(
        Res.string.web_home_open_details,
        content.title,
    )
    val cardModifier = if (focusRequester == null) {
        Modifier
    } else {
        Modifier.focusRequester(focusRequester)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier.width(cardWidth),
    ) {
        StreamCoreWebContentCard(
            onClick = onClick,
            selected = selected,
            aspectRatio = aspectRatio,
            modifier = cardModifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = openDetailsDescription
                }
                .testTag(HomeTestTags.ContentCardPrefix + row.id + ":" + content.id),
        ) {
            StreamCoreWebArtwork(
                imageUrl = imageUrl,
                contentDescription = null,
                fallbackText = content.fallbackText(),
                requestWidthPx = requestWidth,
                requestHeightPx = requestHeight,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (row.type == RowType.TopTen) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = RankSurfaceAlpha),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(StreamCoreDimens.Spacing.Small),
                    ) {
                        Text(
                            text = rank.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = StreamCoreDimens.Spacing.Medium,
                                vertical = StreamCoreDimens.Spacing.Small,
                            ),
                        )
                    }
                }
                content.playbackProgress?.let { progress ->
                    ContinueWatchingProgress(
                        positionMillis = progress.positionMillis,
                        durationMillis = progress.durationMillis,
                    )
                }
            }
        }
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = content.heroMetadata(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BoxScope.ContinueWatchingProgress(
    positionMillis: Long,
    durationMillis: Long,
) {
    val progress = if (durationMillis > 0L) {
        (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Spacing.Small),
    )
}

@Composable
private fun WebHomeLoading() {
    val loadingDescription = stringResource(Res.string.web_home_loading_description)
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Web.Home.SectionSpacing),
        modifier = Modifier
            .fillMaxSize()
            .testTag(WebHomeTestTags.Loading)
            .semantics {
                contentDescription = loadingDescription
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StreamCoreDimens.Web.Home.HeroHeight)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
                Box(
                    modifier = Modifier
                        .width(StreamCoreDimens.Web.Home.LoadingTitleWidth)
                        .height(StreamCoreDimens.Web.Home.LoadingTitleHeight)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(StreamCoreDimens.Web.Home.LandscapeCardWidth)
                                .height(StreamCoreDimens.Web.Home.LoadingCardHeight)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebHomeEmpty(
    focusRequester: FocusRequester,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocusWhenReady()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .testTag(WebHomeTestTags.Empty),
    ) {
        StreamCoreWebPanel {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            ) {
                Text(
                    text = stringResource(Res.string.web_home_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.web_home_empty_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StreamCoreWebButton(
                    text = stringResource(Res.string.web_home_refresh),
                    onClick = onRefresh,
                    modifier = Modifier.focusRequester(focusRequester),
                )
            }
        }
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

@Composable
private fun WebHomePreview(scenario: WebBrowseFixtureScenario) {
    StreamCoreTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            WebHomeScreen(
                state = WebHomePreviewData.stateFor(scenario),
                onAction = {},
                selectedContentKey = null,
                returnFocusKey = null,
                onReturnFocusConsumed = {},
            )
            WebHomePreviewNotice(scenario)
        }
    }
}

@Composable
private fun BoxScope.WebHomePreviewNotice(scenario: WebBrowseFixtureScenario) {
    val title = when (scenario) {
        WebBrowseFixtureScenario.Offline -> stringResource(Res.string.web_home_offline_title)
        WebBrowseFixtureScenario.Error -> stringResource(Res.string.web_home_error_title)
        else -> return
    }
    val message = when (scenario) {
        WebBrowseFixtureScenario.Offline -> stringResource(Res.string.web_home_offline_message)
        WebBrowseFixtureScenario.Error -> stringResource(Res.string.web_home_error_message)
        else -> return
    }
    val testTag = if (scenario == WebBrowseFixtureScenario.Offline) {
        WebHomeTestTags.OfflineNotice
    } else {
        WebHomeTestTags.ErrorNotice
    }
    StreamCoreWebPanel(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(StreamCoreDimens.Spacing.ExtraLarge)
            .testTag(testTag),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebHomeContentPreview() {
    WebHomePreview(WebBrowseFixtureScenario.Content)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebHomeLoadingPreview() {
    WebHomePreview(WebBrowseFixtureScenario.Loading)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebHomeEmptyPreview() {
    WebHomePreview(WebBrowseFixtureScenario.Empty)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebHomeOfflinePreview() {
    WebHomePreview(WebBrowseFixtureScenario.Offline)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebHomeErrorPreview() {
    WebHomePreview(WebBrowseFixtureScenario.Error)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebHomeLongTextPreview() {
    WebHomePreview(WebBrowseFixtureScenario.LongText)
}

private const val HeroRequestWidthPx = 1280
private const val HeroRequestHeightPx = 720
private const val PosterRequestWidthPx = 360
private const val PosterRequestHeightPx = 540
private const val LandscapeRequestWidthPx = 640
private const val LandscapeRequestHeightPx = 360
private const val FocusRequestAttempts = 3
private const val SelectedOverlayAlpha = 0.12f
private const val RankSurfaceAlpha = 0.76f
