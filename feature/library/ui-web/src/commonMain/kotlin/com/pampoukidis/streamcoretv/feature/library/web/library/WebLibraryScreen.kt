package com.pampoukidis.streamcoretv.feature.library.web.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHistoryIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebMediaCard
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import com.pampoukidis.streamcoretv.feature.library.web.testing.WebLibraryFixtures
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.library.ui_web.generated.resources.Res
import streamcoretv.feature.library.ui_web.generated.resources.web_library_continue_watching
import streamcoretv.feature.library.ui_web.generated.resources.web_library_continue_watching_empty
import streamcoretv.feature.library.ui_web.generated.resources.web_library_error_message
import streamcoretv.feature.library.ui_web.generated.resources.web_library_error_title
import streamcoretv.feature.library.ui_web.generated.resources.web_library_liked
import streamcoretv.feature.library.ui_web.generated.resources.web_library_liked_empty
import streamcoretv.feature.library.ui_web.generated.resources.web_library_my_list
import streamcoretv.feature.library.ui_web.generated.resources.web_library_my_list_empty
import streamcoretv.feature.library.ui_web.generated.resources.web_library_offline_title
import streamcoretv.feature.library.ui_web.generated.resources.web_library_refresh
import streamcoretv.feature.library.ui_web.generated.resources.web_library_refreshing
import streamcoretv.feature.library.ui_web.generated.resources.web_library_retry
import streamcoretv.feature.library.ui_web.generated.resources.web_library_title

@Composable
fun WebLibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    selectedContentKey: WebBrowseFocusKey?,
    returnFocusKey: WebBrowseFocusKey?,
    onReturnFocusConsumed: (WebBrowseFocusKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionListState = rememberLazyListState()
    val refreshFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val sections = remember(
        state.continueWatching,
        state.likedContent,
        state.myListContent,
    ) {
        state.toWebLibrarySections()
    }
    val focusRows = remember(sections) {
        sections.map { section ->
            List(section.content.size) { FocusRequester() }
        }
    }
    val exactFocusTarget = remember(state, returnFocusKey) {
        state.findWebLibraryFocusTarget(returnFocusKey)
    }
    val fallbackFocusTarget = remember(state) {
        state.firstWebLibraryFocusTarget()
    }
    var focusAssigned by remember { mutableStateOf(false) }
    val currentOnReturnFocusConsumed by rememberUpdatedState(onReturnFocusConsumed)
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(returnFocusKey) {
        if (returnFocusKey != null) {
            focusAssigned = false
        }
    }

    val requestedFocusTarget = if (focusAssigned) {
        null
    } else {
        exactFocusTarget ?: fallbackFocusTarget
    }
    val requestedFocusKey = returnFocusKey?.takeIf { exactFocusTarget != null }
    val requestedSectionIndex = sections.indexOfFirst { section ->
        section.key == requestedFocusTarget?.sectionKey
    }

    LaunchedEffect(
        state.isLoading,
        requestedFocusTarget,
        requestedSectionIndex,
        state.error,
    ) {
        if (state.isLoading || focusAssigned) {
            return@LaunchedEffect
        }

        if (requestedFocusTarget != null && requestedSectionIndex >= 0) {
            sectionListState.scrollToItem(requestedSectionIndex)
            return@LaunchedEffect
        }

        val fallbackRequester = if (state.error != null) {
            retryFocusRequester
        } else {
            refreshFocusRequester
        }
        if (fallbackRequester.requestFocusWhenReady()) {
            focusAssigned = returnFocusKey == null
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tv.Screen.VerticalPadding,
            )
            .webEscape {
                refreshFocusRequester.requestFocus()
            }
            .testTag(LibraryTestTags.Root),
    ) {
        WebLibraryHeader(
            state = state,
            refreshFocusRequester = refreshFocusRequester,
            downFocusRequester = when {
                state.error != null -> retryFocusRequester
                else -> focusRows.firstContentRequester() ?: FocusRequester.Cancel
            },
            onRefresh = { onAction(LibraryAction.Retry) },
        )

        state.error?.let { error ->
            WebLibraryError(
                error = error,
                retryFocusRequester = retryFocusRequester,
                upFocusRequester = refreshFocusRequester,
                downFocusRequester = focusRows.firstContentRequester() ?: FocusRequester.Cancel,
                onRetry = { onAction(LibraryAction.Retry) },
            )
        }

        when {
            state.isLoading -> WebLibraryLoading(modifier = Modifier.weight(1f))
            else -> LazyColumn(
                state = sectionListState,
                contentPadding = PaddingValues(
                    top = StreamCoreDimens.Spacing.Small,
                    bottom = StreamCoreDimens.Spacing.ExtraLarge,
                ),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(
                    items = sections,
                    key = WebLibrarySection::key,
                    contentType = WebLibrarySection::kind,
                ) { section ->
                    val sectionIndex = sections.indexOf(section)
                    WebLibrarySectionRow(
                        section = section,
                        focusRequesters = focusRows[sectionIndex],
                        upFocusRequester = { itemIndex ->
                            focusRows.previousContentRequester(
                                sectionIndex = sectionIndex,
                                itemIndex = itemIndex,
                            ) ?: if (state.error != null) {
                                retryFocusRequester
                            } else {
                                refreshFocusRequester
                            }
                        },
                        downFocusRequester = { itemIndex ->
                            focusRows.nextContentRequester(
                                sectionIndex = sectionIndex,
                                itemIndex = itemIndex,
                            ) ?: FocusRequester.Cancel
                        },
                        selectedContentKey = selectedContentKey,
                        focusItemIndex = requestedFocusTarget
                            ?.takeIf { target -> target.sectionKey == section.key }
                            ?.itemIndex,
                        focusRequestEnabled = !focusAssigned,
                        focusKeyToConsume = requestedFocusKey
                            ?.takeIf { key -> key.sectionKey == section.key },
                        onFocusAssigned = { consumedKey ->
                            if (returnFocusKey == null || consumedKey != null) {
                                focusAssigned = true
                            }
                            consumedKey?.let(currentOnReturnFocusConsumed)
                        },
                        onContentSelected = { content ->
                            onAction(LibraryAction.ContentSelected(content))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WebLibraryHeader(
    state: LibraryUiState,
    refreshFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onRefresh: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LibraryTestTags.Header),
    ) {
        Text(
            text = stringResource(Res.string.web_library_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        StreamCoreWebButton(
            text = stringResource(
                if (state.isLoading) {
                    Res.string.web_library_refreshing
                } else {
                    Res.string.web_library_refresh
                },
            ),
            onClick = onRefresh,
            enabled = !state.isLoading,
            loading = state.isLoading,
            variant = StreamCoreWebButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(refreshFocusRequester)
                .focusProperties {
                    down = downFocusRequester
                }
                .testTag(LibraryTestTags.Refresh),
        )
    }
}

@Composable
private fun WebLibraryError(
    error: AppError,
    retryFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(StreamCoreDimens.Spacing.Large)
            .testTag(LibraryTestTags.Error),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(
                        if (error is AppError.Network) {
                            Res.string.web_library_offline_title
                        } else {
                            Res.string.web_library_error_title
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.web_library_error_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            StreamCoreWebButton(
                text = stringResource(Res.string.web_library_retry),
                onClick = onRetry,
                variant = StreamCoreWebButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(retryFocusRequester)
                    .focusProperties {
                        up = upFocusRequester
                        down = downFocusRequester
                    }
                    .testTag(LibraryTestTags.Retry),
            )
        }
    }
}

@Composable
private fun WebLibrarySectionRow(
    section: WebLibrarySection,
    focusRequesters: List<FocusRequester>,
    upFocusRequester: (Int) -> FocusRequester,
    downFocusRequester: (Int) -> FocusRequester,
    selectedContentKey: WebBrowseFocusKey?,
    focusItemIndex: Int?,
    focusRequestEnabled: Boolean,
    focusKeyToConsume: WebBrowseFocusKey?,
    onFocusAssigned: (WebBrowseFocusKey?) -> Unit,
    onContentSelected: (ContentModel) -> Unit,
) {
    val rowState = rememberLazyListState()
    val currentOnFocusAssigned by rememberUpdatedState(onFocusAssigned)

    LaunchedEffect(focusItemIndex, focusRequestEnabled, focusKeyToConsume) {
        val itemIndex = focusItemIndex ?: return@LaunchedEffect
        if (!focusRequestEnabled || itemIndex !in focusRequesters.indices) {
            return@LaunchedEffect
        }

        rowState.scrollToItem(itemIndex)
        if (focusRequesters[itemIndex].requestFocusWhenReady()) {
            currentOnFocusAssigned(focusKeyToConsume)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(section.key),
    ) {
        Text(
            text = stringResource(section.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        if (section.content.isEmpty()) {
            WebLibraryEmptySection(section)
        } else {
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(
                    horizontal = StreamCoreDimens.Spacing.Small,
                    vertical = StreamCoreDimens.Spacing.Small,
                ),
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(
                    items = section.content,
                    key = { _, content -> section.key + ":" + content.id },
                    contentType = { _, _ -> section.kind },
                ) { itemIndex, content ->
                    WebLibraryContentCard(
                        content = content,
                        section = section,
                        selected = selectedContentKey.matches(section.key, content.id),
                        onClick = { onContentSelected(content) },
                        modifier = Modifier
                            .focusRequester(focusRequesters[itemIndex])
                            .focusProperties {
                                left = if (itemIndex > 0) {
                                    focusRequesters[itemIndex - 1]
                                } else {
                                    FocusRequester.Cancel
                                }
                                right = if (itemIndex < focusRequesters.lastIndex) {
                                    focusRequesters[itemIndex + 1]
                                } else {
                                    FocusRequester.Cancel
                                }
                                up = upFocusRequester(itemIndex)
                                down = downFocusRequester(itemIndex)
                            }
                            .testTag(
                                LibraryTestTags.ContentPrefix + section.key + ":" + content.id,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WebLibraryContentCard(
    content: ContentModel,
    section: WebLibrarySection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StreamCoreWebMediaCard(
        content = content,
        type = if (section.kind == WebLibrarySectionKind.ContinueWatching) {
            RowType.ContinueWatching
        } else {
            RowType.Poster
        },
        onClick = onClick,
        selected = selected,
        modifier = modifier,
    )
}

@Composable
private fun WebLibraryEmptySection(section: WebLibrarySection) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
            .padding(StreamCoreDimens.Spacing.Large)
            .testTag(LibraryTestTags.EmptyPrefix + section.key),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(StreamCoreDimens.Icon.TouchTarget)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            ) {
                when (section.kind) {
                    WebLibrarySectionKind.ContinueWatching -> StreamCoreHistoryIcon()
                    WebLibrarySectionKind.Liked -> StreamCoreHeartIcon(filled = false)
                    WebLibrarySectionKind.MyList -> StreamCoreBookmarkIcon(filled = false)
                }
            }
            Text(
                text = stringResource(section.emptyMessage),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WebLibraryLoading(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag(LibraryTestTags.Loading),
    ) {
        repeat(LoadingSectionCount) {
            Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
                Box(
                    modifier = Modifier
                        .width(StreamCoreDimens.Tv.Loading.TitleWidth)
                        .height(StreamCoreDimens.Tv.Loading.TitleHeight)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.small,
                        ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
                    repeat(LoadingCardCount) {
                        Box(
                            modifier = Modifier
                                .width(StreamCoreDimens.Tv.Browse.LandscapeCardWidth)
                                .height(StreamCoreDimens.Tv.Browse.LandscapeCardWidth / LandscapeAspectRatio)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.large,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun LibraryUiState.toWebLibrarySections(): List<WebLibrarySection> {
    return listOf(
        WebLibrarySection(
            key = WebLibraryContinueWatchingSection,
            title = Res.string.web_library_continue_watching,
            emptyMessage = Res.string.web_library_continue_watching_empty,
            content = continueWatching,
            kind = WebLibrarySectionKind.ContinueWatching,
        ),
        WebLibrarySection(
            key = WebLibraryLikedSection,
            title = Res.string.web_library_liked,
            emptyMessage = Res.string.web_library_liked_empty,
            content = likedContent,
            kind = WebLibrarySectionKind.Liked,
        ),
        WebLibrarySection(
            key = WebLibraryMyListSection,
            title = Res.string.web_library_my_list,
            emptyMessage = Res.string.web_library_my_list_empty,
            content = myListContent,
            kind = WebLibrarySectionKind.MyList,
        ),
    )
}

private fun List<List<FocusRequester>>.firstContentRequester(): FocusRequester? {
    forEach { row ->
        row.firstOrNull()?.let { requester -> return requester }
    }
    return null
}

private fun List<List<FocusRequester>>.previousContentRequester(
    sectionIndex: Int,
    itemIndex: Int,
): FocusRequester? {
    for (index in sectionIndex - 1 downTo 0) {
        val row = get(index)
        if (row.isNotEmpty()) {
            return row[itemIndex.coerceAtMost(row.lastIndex)]
        }
    }
    return null
}

private fun List<List<FocusRequester>>.nextContentRequester(
    sectionIndex: Int,
    itemIndex: Int,
): FocusRequester? {
    for (index in sectionIndex + 1..lastIndex) {
        val row = get(index)
        if (row.isNotEmpty()) {
            return row[itemIndex.coerceAtMost(row.lastIndex)]
        }
    }
    return null
}

private fun WebBrowseFocusKey?.matches(
    sectionKey: String,
    itemKey: String,
): Boolean {
    return this?.destination == WebBrowseDestination.Library &&
        this.sectionKey == sectionKey &&
        this.itemKey == itemKey
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

private data class WebLibrarySection(
    val key: String,
    val title: StringResource,
    val emptyMessage: StringResource,
    val content: List<ContentModel>,
    val kind: WebLibrarySectionKind,
)

private enum class WebLibrarySectionKind {
    ContinueWatching,
    Liked,
    MyList,
}

private const val LoadingSectionCount = 3
private const val LoadingCardCount = 4
private const val FocusRequestAttempts = 3
private val LandscapeAspectRatio = 16f / 9f

@Preview(name = "Library · Loading", widthDp = 1280, heightDp = 720)
@Composable
private fun WebLibraryLoadingPreview() {
    WebLibraryPreview(WebBrowseFixtureScenario.Loading)
}

@Preview(name = "Library · Content", widthDp = 1280, heightDp = 720)
@Composable
private fun WebLibraryContentPreview() {
    WebLibraryPreview(WebBrowseFixtureScenario.Content)
}

@Preview(name = "Library · Empty", widthDp = 1280, heightDp = 720)
@Composable
private fun WebLibraryEmptyPreview() {
    WebLibraryPreview(WebBrowseFixtureScenario.Empty)
}

@Preview(name = "Library · Offline", widthDp = 1280, heightDp = 720)
@Composable
private fun WebLibraryOfflinePreview() {
    WebLibraryPreview(WebBrowseFixtureScenario.Offline)
}

@Preview(name = "Library · Error", widthDp = 1280, heightDp = 720)
@Composable
private fun WebLibraryErrorPreview() {
    WebLibraryPreview(WebBrowseFixtureScenario.Error)
}

@Preview(name = "Library · Long text", widthDp = 1280, heightDp = 720)
@Composable
private fun WebLibraryLongTextPreview() {
    WebLibraryPreview(WebBrowseFixtureScenario.LongText)
}

@Composable
private fun WebLibraryPreview(scenario: WebBrowseFixtureScenario) {
    StreamCoreTheme(darkTheme = true) {
        WebLibraryScreen(
            state = WebLibraryFixtures.state(scenario),
            onAction = {},
            selectedContentKey = null,
            returnFocusKey = null,
            onReturnFocusConsumed = {},
        )
    }
}
