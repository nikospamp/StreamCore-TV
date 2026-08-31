package com.pampoukidis.streamcoretv.feature.library.mobile.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHistoryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreMobileContinueWatchingCard
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreMobilePosterCard
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePersonIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import com.pampoukidis.streamcoretv.feature.library.mobile.R

@Composable
fun MobileLibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
    activeProfile: ProfileModel? = null,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomPadding = StreamCoreDimens.Mobile.Navigation.Height +
            StreamCoreDimens.Spacing.ExtraLarge + navigationInset

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(LibraryTestTags.Root),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Mobile.Browse.SectionSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header", contentType = "header") {
                LibraryHeader(
                    activeProfile = activeProfile,
                    onProfileSelected = onProfileSelected,
                )
            }

            if (state.error != null) {
                item(key = "error", contentType = "error") {
                    LibraryError(onRetry = { onAction(LibraryAction.Retry) })
                }
            }

            if (state.isLoading) {
                item(key = "loading", contentType = "loading") {
                    LibraryLoadingState()
                }
            } else {
                item(key = "continue-watching", contentType = "continue-watching") {
                    LibrarySection(
                        title = stringResource(R.string.library_continue_watching),
                        emptyMessage = stringResource(R.string.library_continue_watching_empty),
                        emptyTag = LibraryTestTags.ContinueWatching,
                        items = state.continueWatching,
                        sectionTag = LibraryTestTags.ContinueWatching,
                        emptyIcon = { StreamCoreHistoryIcon() },
                    ) { content ->
                        StreamCoreMobileContinueWatchingCard(
                            content = content,
                            onClick = { onAction(LibraryAction.ContentSelected(content)) },
                            selectedContentKey = selectedContentKey,
                            sharedElementScope = sharedElementScope,
                            modifier = Modifier.testTag(contentTag(content)),
                        )
                    }
                }
                item(key = "liked", contentType = "liked") {
                    LibrarySection(
                        title = stringResource(R.string.library_liked),
                        emptyMessage = stringResource(R.string.library_liked_empty),
                        emptyTag = LibraryTestTags.Liked,
                        items = state.likedContent,
                        sectionTag = LibraryTestTags.Liked,
                        emptyIcon = { StreamCoreHeartIcon(filled = false) },
                    ) { content ->
                        StreamCoreMobilePosterCard(
                            content = content,
                            onClick = { onAction(LibraryAction.ContentSelected(content)) },
                            selectedContentKey = selectedContentKey,
                            sharedElementScope = sharedElementScope,
                            modifier = Modifier.testTag(contentTag(content)),
                        )
                    }
                }
                item(key = "my-list", contentType = "my-list") {
                    LibrarySection(
                        title = stringResource(R.string.library_my_list),
                        emptyMessage = stringResource(R.string.library_my_list_empty),
                        emptyTag = LibraryTestTags.MyList,
                        items = state.myListContent,
                        sectionTag = LibraryTestTags.MyList,
                        emptyIcon = { StreamCoreBookmarkIcon(filled = false) },
                    ) { content ->
                        StreamCoreMobilePosterCard(
                            content = content,
                            onClick = { onAction(LibraryAction.ContentSelected(content)) },
                            selectedContentKey = selectedContentKey,
                            sharedElementScope = sharedElementScope,
                            modifier = Modifier.testTag(contentTag(content)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    activeProfile: ProfileModel?,
    onProfileSelected: () -> Unit,
) {
    val chooseProfileDescription = stringResource(R.string.library_choose_profile)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(HeaderHeight)
            .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding)
            .testTag(LibraryTestTags.Header),
    ) {
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            onClick = onProfileSelected,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .size(StreamCoreDimens.Icon.TouchTarget)
                .semantics {
                    contentDescription = chooseProfileDescription
                }
                .testTag(LibraryTestTags.Profile),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (activeProfile != null) {
                    StreamCoreProfileArtwork(
                        avatar = activeProfile.avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(StreamCoreDimens.Icon.Large)
                            .clip(CircleShape),
                    )
                } else {
                    StreamCorePersonIcon(
                        modifier = Modifier.size(StreamCoreDimens.Icon.Medium),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryError(onRetry: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(
                horizontal = StreamCoreDimens.Spacing.Large,
                vertical = StreamCoreDimens.Spacing.Medium,
            )
            .testTag(LibraryTestTags.Error),
    ) {
        Text(
            text = stringResource(R.string.library_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        StreamCoreTextButton(
            text = stringResource(R.string.library_retry),
            onClick = onRetry,
            enabled = true,
            modifier = Modifier.testTag(LibraryTestTags.Retry),
        )
    }
}

@Composable
private fun LibrarySection(
    title: String,
    emptyMessage: String,
    emptyTag: String,
    items: List<ContentModel>,
    sectionTag: String,
    emptyIcon: @Composable () -> Unit,
    content: @Composable (ContentModel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Mobile.Browse.RowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(sectionTag),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
        )
        if (items.isEmpty()) {
            EmptyLibrarySection(
                message = emptyMessage,
                icon = emptyIcon,
                modifier = Modifier.testTag(LibraryTestTags.EmptyPrefix + emptyTag),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Mobile.Browse.RowSpacing),
            ) {
                items(
                    items = items,
                    key = { item -> "${item.row}:${item.id}" },
                    contentType = { "library-content" },
                ) { item -> content(item) }
            }
        }
    }
}

@Composable
private fun EmptyLibrarySection(
    message: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(StreamCoreDimens.Spacing.Large),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(StreamCoreDimens.Icon.TouchTarget)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                ),
        ) {
            icon()
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryLoadingState() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Mobile.Browse.SectionSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LibraryTestTags.Loading),
    ) {
        LoadingShelf(cardWidth = StreamCoreDimens.Mobile.Browse.ContinueWatchingWidth)
        LoadingShelf(cardWidth = StreamCoreDimens.Mobile.Browse.PosterWidth)
        LoadingShelf(cardWidth = StreamCoreDimens.Mobile.Browse.PosterWidth)
    }
}

@Composable
private fun LoadingShelf(cardWidth: Dp) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Mobile.Browse.RowSpacing),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding)
                .width(TitleSkeletonWidth)
                .height(TitleSkeletonHeight)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                ),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Mobile.Browse.RowSpacing),
            modifier = Modifier.padding(start = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
        ) {
            repeat(LoadingCardCount) {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(if (cardWidth == StreamCoreDimens.Mobile.Browse.PosterWidth) PosterSkeletonHeight else LandscapeSkeletonHeight)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.medium,
                        ),
                )
            }
        }
    }
}

private fun contentTag(content: ContentModel): String {
    return LibraryTestTags.ContentPrefix + content.row + ":" + content.id
}

private val HeaderHeight = StreamCoreDimens.Icon.TouchTarget + StreamCoreDimens.Spacing.Small
private val TitleSkeletonWidth = StreamCoreDimens.Mobile.Library.LoadingTitleWidth
private val TitleSkeletonHeight = StreamCoreDimens.Mobile.Library.LoadingTitleHeight
private val PosterSkeletonHeight = StreamCoreDimens.Mobile.Library.LoadingPosterHeight
private val LandscapeSkeletonHeight = StreamCoreDimens.Mobile.Library.LoadingLandscapeHeight
private const val LoadingCardCount = 3

@PreviewMobile
@Composable
private fun MobileLibraryScreenDarkPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileLibraryScreen(
            state = LibraryPreviewData.contentState,
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileLibraryScreenLightPreview() {
    StreamCoreTheme(darkTheme = false) {
        MobileLibraryScreen(
            state = LibraryPreviewData.contentState,
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileLibraryScreenEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileLibraryScreen(
            state = LibraryUiState(isLoading = false),
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileLibraryScreenLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileLibraryScreen(
            state = LibraryUiState(isLoading = true),
            onAction = {},
            onProfileSelected = {},
        )
    }
}
