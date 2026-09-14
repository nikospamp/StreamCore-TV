package com.pampoukidis.streamcoretv.feature.library.tablet.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBookmarkIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSharedArtworkImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHistoryIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePersonIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreOverlayDuringSharedTransition
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import com.pampoukidis.streamcoretv.feature.library.tablet.R

@Composable
fun TabletLibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
    activeProfile: ProfileModel? = null,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val sections = remember(state) { state.toSections() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = StreamCoreDimens.Tablet.Screen.VerticalPadding),
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.SectionSpacing),
            modifier = Modifier
                .fillMaxSize()
                .testTag(LibraryTestTags.Root),
        ) {
            item(key = HeaderKey, contentType = HeaderContentType) {
                TabletLibraryHeader(
                    activeProfile = activeProfile,
                    onProfileSelected = onProfileSelected,
                )
            }

            if (state.error != null) {
                item(key = ErrorKey, contentType = ErrorContentType) {
                    TabletLibraryError(onRetry = { onAction(LibraryAction.Retry) })
                }
            }

            if (state.isLoading) {
                item(key = LoadingKey, contentType = LoadingContentType) {
                    TabletLibraryLoading()
                }
            } else {
                items(
                    items = sections,
                    key = { section -> section.key },
                    contentType = { section -> section.contentType },
                ) { section ->
                    TabletLibrarySection(
                        section = section,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                        onSelected = { content ->
                            onAction(
                                LibraryAction.ContentSelected(
                                    content = content,
                                    sourceArtworkUrl = content.tabletLibraryImageUrl(section.aspectRatio),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletLibraryHeader(
    activeProfile: ProfileModel?,
    onProfileSelected: () -> Unit,
) {
    val chooseProfileDescription = stringResource(R.string.library_choose_profile)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tablet.Screen.VerticalPadding,
            )
            .testTag(LibraryTestTags.Header),
    ) {
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onProfileSelected,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .size(StreamCoreDimens.Icon.TouchTarget)
                .semantics { contentDescription = chooseProfileDescription }
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
                    StreamCorePersonIcon(modifier = Modifier.size(StreamCoreDimens.Icon.Medium))
                }
            }
        }
    }
}

@Composable
private fun TabletLibraryError(onRetry: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding)
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
        StreamCoreTextButton(
            text = stringResource(R.string.library_retry),
            onClick = onRetry,
            enabled = true,
            modifier = Modifier.testTag(LibraryTestTags.Retry),
        )
    }
}

@Composable
private fun TabletLibrarySection(
    section: TabletLibrarySectionModel,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
    onSelected: (ContentModel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.RowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(section.testTag),
    ) {
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            ),
        )
        if (section.items.isEmpty()) {
            TabletLibraryEmptySection(section = section)
        } else {
            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.RowSpacing),
            ) {
                items(
                    items = section.items,
                    key = { content -> "${content.row}:${content.id}" },
                    contentType = { section.contentType },
                ) { content ->
                    val contentKey = StreamCoreSharedKey.content(
                        contentId = content.id,
                        row = content.row,
                    )
                    TabletLibraryCard(
                        content = content,
                        cardWidth = section.cardWidth,
                        aspectRatio = section.aspectRatio,
                        onClick = { onSelected(content) },
                        sharedElementScope = sharedElementScope.takeIf {
                            selectedContentKey == contentKey
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletLibraryCard(
    content: ContentModel,
    cardWidth: Dp,
    aspectRatio: Float,
    onClick: () -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    val openContentDescription = stringResource(R.string.library_open_content, content.title)
    val shape = MaterialTheme.shapes.large
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = Modifier
            .width(cardWidth)
            .semantics { contentDescription = openContentDescription }
            .clickable(onClick = onClick)
            .testTag(LibraryTestTags.ContentPrefix + content.row + ":" + content.id),
    ) {
        StreamCoreSharedArtworkImage(
            imageUrl = content.tabletLibraryImageUrl(aspectRatio),
            contentDescription = null,
            fallbackText = content.fallbackText(),
            sharedKey = StreamCoreSharedKey.artwork(content.id, content.row),
            clipShape = shape,
            sharedElementScope = sharedElementScope,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
        )
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.streamCoreOverlayDuringSharedTransition(
                sharedElementScope = sharedElementScope,
                zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
            ).clip(MaterialTheme.shapes.small),
        )
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun ContentModel.tabletLibraryImageUrl(aspectRatio: Float): String? {
    if (aspectRatio == StreamCoreDimens.Artwork.LandscapeAspectRatio) {
        return backdrop ?: poster
    }
    return poster
}

@Composable
private fun TabletLibraryEmptySection(section: TabletLibrarySectionModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding)
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
                .size(StreamCoreDimens.Icon.TouchTarget)
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
private fun TabletLibraryLoading() {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Tablet.Browse.SectionSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding)
            .testTag(LibraryTestTags.Loading),
    ) {
        LoadingShelf(
            cardWidth = StreamCoreDimens.Tablet.Browse.LandscapeWidth,
            aspectRatio = StreamCoreDimens.Artwork.LandscapeAspectRatio,
        )
        LoadingShelf(
            cardWidth = StreamCoreDimens.Tablet.Browse.PosterWidth,
            aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
        )
        LoadingShelf(
            cardWidth = StreamCoreDimens.Tablet.Browse.PosterWidth,
            aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
        )
    }
}

@Composable
private fun LoadingShelf(cardWidth: Dp, aspectRatio: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
        Box(
            modifier = Modifier
                .width(StreamCoreDimens.Tablet.Browse.PosterWidth)
                .height(StreamCoreDimens.Icon.Standard)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large)) {
            repeat(LoadingCardCount) {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .aspectRatio(aspectRatio)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

private fun LibraryUiState.toSections(): List<TabletLibrarySectionModel> {
    return listOf(
        TabletLibrarySectionModel(
            key = "continue-watching",
            titleRes = R.string.library_continue_watching,
            emptyMessageRes = R.string.library_continue_watching_empty,
            testTag = LibraryTestTags.ContinueWatching,
            items = continueWatching,
            contentType = "library-continue-watching",
            cardWidth = StreamCoreDimens.Tablet.Browse.LandscapeWidth,
            aspectRatio = StreamCoreDimens.Artwork.LandscapeAspectRatio,
            icon = { StreamCoreHistoryIcon() },
        ),
        TabletLibrarySectionModel(
            key = "liked",
            titleRes = R.string.library_liked,
            emptyMessageRes = R.string.library_liked_empty,
            testTag = LibraryTestTags.Liked,
            items = likedContent,
            contentType = "library-liked",
            cardWidth = StreamCoreDimens.Tablet.Browse.PosterWidth,
            aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
            icon = { StreamCoreHeartIcon(filled = false) },
        ),
        TabletLibrarySectionModel(
            key = "my-list",
            titleRes = R.string.library_my_list,
            emptyMessageRes = R.string.library_my_list_empty,
            testTag = LibraryTestTags.MyList,
            items = myListContent,
            contentType = "library-my-list",
            cardWidth = StreamCoreDimens.Tablet.Browse.PosterWidth,
            aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
            icon = { StreamCoreBookmarkIcon(filled = false) },
        ),
    )
}

private data class TabletLibrarySectionModel(
    val key: String,
    val titleRes: Int,
    val emptyMessageRes: Int,
    val testTag: String,
    val items: List<ContentModel>,
    val contentType: String,
    val cardWidth: Dp,
    val aspectRatio: Float,
    val icon: @Composable () -> Unit,
)

private const val HeaderKey = "header"
private const val ErrorKey = "error"
private const val LoadingKey = "loading"
private const val HeaderContentType = "header"
private const val ErrorContentType = "error"
private const val LoadingContentType = "loading"
private const val LoadingCardCount = 4

@PreviewTablet
@Composable
private fun TabletLibraryContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletLibraryScreen(
            state = LibraryPreviewData.contentState,
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletLibraryEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletLibraryScreen(
            state = LibraryUiState(isLoading = false),
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletLibraryLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletLibraryScreen(
            state = LibraryUiState(isLoading = true),
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletLibraryErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletLibraryScreen(
            state = LibraryUiState(
                isLoading = false,
                error = AppError.Network(),
            ),
            onAction = {},
            onProfileSelected = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletLibraryLongTextPreview() {
    val content = LibraryPreviewData.contentState
    StreamCoreTheme(darkTheme = true) {
        TabletLibraryScreen(
            state = content.copy(
                likedContent = content.likedContent.mapIndexed { index, item ->
                    if (index == 0) {
                        item.copy(title = "Η τελευταία αποστολή πέρα από τον ορατό ορίζοντα")
                    } else {
                        item
                    }
                },
            ),
            onAction = {},
            onProfileSelected = {},
        )
    }
}
