package com.pampoukidis.streamcoretv.feature.library.tv.library

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHistoryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvContentCard
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import com.pampoukidis.streamcoretv.feature.library.tv.R

@Composable
fun TvLibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    returnFocusKey: String? = null,
    onReturnFocusConsumed: (String) -> Unit = {},
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val listState = rememberLazyListState()
    val refreshFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    var hasAssignedFocus by remember { mutableStateOf(false) }
    val sections = remember(state) { state.toSections() }
    val focusLocation = remember(sections, returnFocusKey) {
        sections.findFocusLocation(returnFocusKey)
    }
    val sectionStartIndex = 1 + if (state.error != null) 1 else 0

    LaunchedEffect(focusLocation, state.isLoading, state.error, returnFocusKey, hasAssignedFocus) {
        if (state.isLoading || hasAssignedFocus) {
            return@LaunchedEffect
        }
        when {
            focusLocation != null -> {
                listState.scrollToItem(sectionStartIndex + focusLocation.sectionIndex)
            }

            state.error != null -> {
                if (retryFocusRequester.requestFocusWhenReady()) {
                    returnFocusKey?.let { focusKey ->
                        hasAssignedFocus = true
                        onReturnFocusConsumed(focusKey)
                    }
                }
            }

            else -> {
                if (refreshFocusRequester.requestFocusWhenReady()) {
                    returnFocusKey?.let { focusKey ->
                        hasAssignedFocus = true
                        onReturnFocusConsumed(focusKey)
                    }
                }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(LibraryTestTags.Root),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = StreamCoreDimens.Tv.Screen.VerticalPadding,
                bottom = StreamCoreDimens.Tv.Screen.VerticalPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer(),
        ) {
            item(key = "header", contentType = "header") {
                TvLibraryHeader(
                    isLoading = state.isLoading,
                    onRefresh = { onAction(LibraryAction.Retry) },
                    refreshFocusRequester = refreshFocusRequester,
                )
            }
            if (state.error != null) {
                item(key = "error", contentType = "error") {
                    TvLibraryError(
                        onRetry = { onAction(LibraryAction.Retry) },
                        focusRequester = retryFocusRequester,
                    )
                }
            }
            if (state.isLoading) {
                item(key = "loading", contentType = "loading") {
                    TvLibraryLoading()
                }
            } else {
                itemsIndexed(
                    items = sections,
                    key = { _, section -> section.key },
                    contentType = { _, section -> section.type },
                ) { sectionIndex, section ->
                    TvLibrarySection(
                        section = section,
                        focusContentIndex = if (focusLocation?.sectionIndex == sectionIndex) {
                            focusLocation.contentIndex
                        } else {
                            null
                        },
                        onSelected = { content ->
                            onAction(LibraryAction.ContentSelected(content))
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
private fun TvLibraryHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    refreshFocusRequester: FocusRequester,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            )
            .testTag(LibraryTestTags.Header),
    ) {
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        StreamCoreTvButton(
            text = stringResource(R.string.library_refresh),
            onClick = onRefresh,
            enabled = !isLoading,
            variant = StreamCoreTvButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(refreshFocusRequester)
                .testTag(LibraryTestTags.Refresh),
        )
    }
}

@Composable
private fun TvLibraryError(
    onRetry: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            )
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(StreamCoreDimens.Spacing.Large)
            .testTag(LibraryTestTags.Error),
    ) {
        Text(
            text = stringResource(R.string.library_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        StreamCoreTvButton(
            text = stringResource(R.string.library_retry),
            onClick = onRetry,
            enabled = true,
            variant = StreamCoreTvButtonVariant.Secondary,
            modifier = Modifier
                .focusRequester(focusRequester)
                .testTag(LibraryTestTags.Retry),
        )
    }
}

@Composable
private fun TvLibrarySection(
    section: TvLibrarySectionModel,
    focusContentIndex: Int?,
    onSelected: (ContentModel) -> Unit,
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
        focusContentIndex?.let { index ->
            rowState.scrollToItem(index)
            if (focusRequester.requestFocusWhenReady()) {
                onFocusAssigned(returnFocusKey)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(section.testTag),
    ) {
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            ),
        )
        if (section.items.isEmpty()) {
            TvLibraryEmptySection(section)
        } else {
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
                    items = section.items,
                    key = { _, content -> "${content.row}:${content.id}" },
                    contentType = { _, _ -> section.type },
                ) { index, content ->
                    StreamCoreTvContentCard(
                        content = content,
                        type = section.type,
                        onClick = { onSelected(content) },
                        focusRequester = if (index == focusContentIndex) {
                            focusRequester
                        } else {
                            null
                        },
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.testTag(
                            LibraryTestTags.ContentPrefix + content.row + ":" + content.id,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TvLibraryEmptySection(section: TvLibrarySectionModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(StreamCoreDimens.Spacing.Large)
            .testTag(LibraryTestTags.EmptyPrefix + section.testTag),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(StreamCoreDimens.Icon.TouchTarget)
                .height(StreamCoreDimens.Icon.TouchTarget)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                ),
        ) {
            section.icon()
        }
        Text(
            text = stringResource(section.emptyMessageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TvLibraryLoading() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = StreamCoreDimens.Tv.Navigation.ContentStartPadding,
                end = StreamCoreDimens.Tv.Screen.HorizontalPadding,
            )
            .testTag(LibraryTestTags.Loading),
    ) {
        repeat(3) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            ) {
                Box(
                    modifier = Modifier
                        .width(StreamCoreDimens.Tv.Loading.TitleWidth)
                        .height(StreamCoreDimens.Tv.Loading.TitleHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(StreamCoreDimens.Tv.Browse.LandscapeCardWidth)
                                .height(
                                    StreamCoreDimens.Tv.Browse.LandscapeCardWidth /
                                            StreamCoreDimens.Artwork.LandscapeAspectRatio,
                                )
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }
        }
    }
}

private fun LibraryUiState.toSections(): List<TvLibrarySectionModel> {
    return listOf(
        TvLibrarySectionModel(
            key = "continue-watching",
            titleRes = R.string.library_continue_watching,
            emptyMessageRes = R.string.library_continue_watching_empty,
            testTag = LibraryTestTags.ContinueWatching,
            items = continueWatching,
            type = RowType.ContinueWatching,
            icon = { StreamCoreHistoryIcon() },
        ),
        TvLibrarySectionModel(
            key = "liked",
            titleRes = R.string.library_liked,
            emptyMessageRes = R.string.library_liked_empty,
            testTag = LibraryTestTags.Liked,
            items = likedContent,
            type = RowType.Poster,
            icon = { StreamCoreHeartIcon(filled = false) },
        ),
        TvLibrarySectionModel(
            key = "my-list",
            titleRes = R.string.library_my_list,
            emptyMessageRes = R.string.library_my_list_empty,
            testTag = LibraryTestTags.MyList,
            items = myListContent,
            type = RowType.Poster,
            icon = { StreamCoreBookmarkIcon(filled = false) },
        ),
    )
}

private fun List<TvLibrarySectionModel>.findFocusLocation(
    selectedContentKey: String?,
): TvLibraryFocusLocation? {
    if (selectedContentKey != null) {
        forEachIndexed { sectionIndex, section ->
            val contentIndex = section.items.indexOfFirst { content ->
                StreamCoreSharedKey.content(
                    contentId = content.id,
                    row = content.row,
                ) == selectedContentKey
            }
            if (contentIndex >= 0) {
                return TvLibraryFocusLocation(sectionIndex, contentIndex)
            }
        }
    }
    val firstSectionIndex = indexOfFirst { section -> section.items.isNotEmpty() }
    if (firstSectionIndex < 0) {
        return null
    }
    return TvLibraryFocusLocation(firstSectionIndex, 0)
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

private data class TvLibraryFocusLocation(
    val sectionIndex: Int,
    val contentIndex: Int,
)

private const val FocusRequestAttempts = 3

private data class TvLibrarySectionModel(
    val key: String,
    val titleRes: Int,
    val emptyMessageRes: Int,
    val testTag: String,
    val items: List<ContentModel>,
    val type: RowType,
    val icon: @Composable () -> Unit,
)

@PreviewTV
@Composable
private fun TvLibraryScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvLibraryScreen(
            state = LibraryPreviewData.contentState,
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvLibraryEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvLibraryScreen(
            state = LibraryUiState(isLoading = false),
            onAction = {},
        )
    }
}
