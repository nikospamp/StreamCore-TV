package com.pampoukidis.streamcoretv.feature.home.web.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCarouselIndicator
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreInfoIcon
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebArtworkIconButton
import androidx.compose.ui.draw.rotate
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebMediaCard
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeHeroArtwork
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeHeroContent
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.home.toHomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import com.pampoukidis.streamcoretv.feature.home.web.testing.WebHomePreviewData
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.home.ui_web.generated.resources.Res
import streamcoretv.feature.home.ui_web.generated.resources.web_home_empty_title
import streamcoretv.feature.home.ui_web.generated.resources.web_home_error_message
import streamcoretv.feature.home.ui_web.generated.resources.web_home_error_title
import streamcoretv.feature.home.ui_web.generated.resources.web_home_featured_movie
import streamcoretv.feature.home.ui_web.generated.resources.web_home_loading_description
import streamcoretv.feature.home.ui_web.generated.resources.web_home_more_details
import streamcoretv.feature.home.ui_web.generated.resources.web_home_next_feature
import streamcoretv.feature.home.ui_web.generated.resources.web_home_offline_message
import streamcoretv.feature.home.ui_web.generated.resources.web_home_offline_title
import streamcoretv.feature.home.ui_web.generated.resources.web_home_open_details
import streamcoretv.feature.home.ui_web.generated.resources.web_home_previous_feature
import streamcoretv.feature.home.ui_web.generated.resources.web_home_retry

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
                    verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
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
private fun WebHomeHero(
    items: List<ContentModel>,
    onAction: (HomeAction) -> Unit,
    returnFocusKey: WebBrowseFocusKey?,
    returnContentIndex: Int?,
    requestInitialFocus: Boolean,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
) {
    var activeIndex by rememberSaveable(items.map(ContentModel::id)) { mutableIntStateOf(0) }
    val detailsFocusRequester = remember { FocusRequester() }
    val safeActiveIndex = activeIndex.coerceIn(items.indices)
    val activeContent = items[safeActiveIndex]
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = StreamCoreDimens.Tv.Browse.HeroHeight)
            .testTag(HomeTestTags.Hero),
    ) {
        HomeHeroArtwork(
            content = activeContent,
            modifier = Modifier.matchParentSize(),
        )
        HomeHeroContent(
            content = activeContent,
            featuredLabel = stringResource(Res.string.web_home_featured_movie),
            contentPadding = PaddingValues(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Spacing.ExtraLarge,
                top = StreamCoreDimens.Tv.Screen.VerticalPadding,
                bottom = if (maxWidth < StreamCoreDimens.Web.Home.HeroCopyMaxWidth && items.size > 1) {
                    StreamCoreDimens.Tv.Screen.VerticalPadding + StreamCoreDimens.Icon.TouchTarget
                } else {
                    StreamCoreDimens.Tv.Screen.VerticalPadding
                },
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width((maxWidth * 0.62f).coerceAtLeast(minOf(maxWidth, StreamCoreDimens.Web.Home.HeroCopyMaxWidth))),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StreamCoreWebButton(
                    text = stringResource(Res.string.web_home_more_details),
                    onClick = { onAction(HomeAction.ContentSelected(activeContent)) },
                    leadingIcon = { StreamCoreInfoIcon() },
                    minHeight = StreamCoreDimens.Button.CompactHeight,
                    contentPadding = PaddingValues(horizontal = StreamCoreDimens.Button.CompactHorizontalPadding),
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
            }
        }
        if (items.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(StreamCoreDimens.Spacing.ExtraLarge),
            ) {
                StreamCoreWebArtworkIconButton(
                    contentDescription = stringResource(Res.string.web_home_previous_feature),
                    onClick = { activeIndex = (safeActiveIndex - 1 + items.size) % items.size },
                    modifier = Modifier.testTag(WebHomeTestTags.PreviousFeature),
                ) { StreamCoreBackIcon() }
                StreamCoreCarouselIndicator(
                    itemCount = items.size,
                    activeItemIndex = safeActiveIndex,
                    modifier = Modifier.testTag(WebHomeTestTags.HeroPosition),
                )
                StreamCoreWebArtworkIconButton(
                    contentDescription = stringResource(Res.string.web_home_next_feature),
                    onClick = { activeIndex = (safeActiveIndex + 1) % items.size },
                    modifier = Modifier.testTag(WebHomeTestTags.NextFeature),
                ) { StreamCoreBackIcon(modifier = Modifier.rotate(180f)) }
            }
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
        Text(
            text = row.title,
            modifier = Modifier.padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            contentPadding = PaddingValues(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                top = StreamCoreDimens.Spacing.Small,
                bottom = StreamCoreDimens.Spacing.Small,
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
    val openDetailsDescription = stringResource(Res.string.web_home_open_details, content.title)
    val cardModifier = if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester)
    StreamCoreWebMediaCard(
        content = content,
        type = row.type,
        onClick = onClick,
        selected = selected,
        rank = rank,
        modifier = cardModifier
            .semantics { contentDescription = openDetailsDescription }
            .testTag(HomeTestTags.ContentCardPrefix + row.id + ":" + content.id),
    )
}

@Composable
private fun WebHomeLoading() {
    val loadingDescription = stringResource(Res.string.web_home_loading_description)
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            )
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
        Box(
            modifier = Modifier
                .width(StreamCoreDimens.Tv.Loading.TitleWidth)
                .height(StreamCoreDimens.Tv.Loading.TitleHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
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
        Box(modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            ) {
                Text(
                    text = stringResource(Res.string.web_home_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StreamCoreWebButton(
                    text = stringResource(Res.string.web_home_retry),
                    onClick = onRefresh,
                    modifier = Modifier.focusRequester(focusRequester).testTag(HomeTestTags.RefreshButton),
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

private const val FocusRequestAttempts = 3
