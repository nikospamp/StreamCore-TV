package com.pampoukidis.streamcoretv.feature.home.tv.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.model.content.imageUrl
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags

@Composable
fun TvHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val firstContentFocusRequester = remember { FocusRequester() }
    val firstContentId = state.rows.firstOrNull()?.content?.firstOrNull()?.id

    LaunchedEffect(firstContentId) {
        if (firstContentId != null) {
            firstContentFocusRequester.requestFocus()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = StreamCoreDimens.Tv.Screen.VerticalPadding),
        ) {
            HomeHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                ),
            )
            if (state.isLoading && state.rows.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
                )
            }
            TvHomeBody(
                state = state,
                onAction = onAction,
                firstContentFocusRequester = firstContentFocusRequester,
                selectedContentKey = selectedContentKey,
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
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Featured for your profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StreamCoreTvButton(
            text = "Refresh",
            onClick = { onAction(HomeAction.Refresh) },
            enabled = !isLoading,
            modifier = Modifier.testTag(HomeTestTags.RefreshButton),
        )
    }
}

@Composable
private fun TvHomeBody(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    firstContentFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.rows.isEmpty() -> CircularProgressIndicator()
            state.rows.isEmpty() -> Text(
                text = "No content available.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Tv.Screen.VerticalPadding),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = state.rows,
                    key = { _, row -> row.id },
                    contentType = { _, row -> row.type },
                ) { rowIndex, row ->
                    TvContentRow(
                        row = row,
                        onAction = onAction,
                        firstContentFocusRequester = if (rowIndex == 0) {
                            firstContentFocusRequester
                        } else {
                            null
                        },
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvContentRow(
    row: RowModel,
    onAction: (HomeAction) -> Unit,
    firstContentFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Tv.Focus.BorderPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.type },
            ) { contentIndex, content ->
                TvContentCard(
                    rowId = row.id,
                    content = content,
                    type = row.type,
                    rank = contentIndex + 1,
                    onClick = {
                        onAction(HomeAction.ContentSelected(content))
                    },
                    focusRequester = if (contentIndex == 0) {
                        firstContentFocusRequester
                    } else {
                        null
                    },
                    useSharedTransition = StreamCoreSharedKey.content(
                        contentId = content.id,
                        row = content.row,
                    ) == selectedContentKey,
                    sharedElementScope = sharedElementScope,
                )
            }
        }
    }
}

@Composable
private fun TvContentCard(
    rowId: String,
    content: ContentModel,
    type: RowType,
    rank: Int,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    useSharedTransition: Boolean,
    sharedElementScope: StreamCoreSharedElementScope?,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val spec = type.tvCardSpec()
    val imageShape = MaterialTheme.shapes.large
    val elementScope = if (useSharedTransition) {
        sharedElementScope
    } else {
        null
    }
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { it.name }
    }
    val focusModifier = if (focusRequester == null) {
        Modifier
    } else {
        Modifier.focusRequester(focusRequester)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) {
            StreamCoreDimens.Elevation.Medium
        } else {
            StreamCoreDimens.Elevation.Low
        },
        border = if (isFocused) {
            BorderStroke(
                width = StreamCoreDimens.Tv.Focus.BorderWidth,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
        modifier = modifier
            .width(spec.width)
            .then(focusModifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
            StreamCoreContentImage(
                imageUrl = content.imageUrl(type),
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isFocused) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fallbackTextStyle = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(spec.aspectRatio)
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = StreamCoreSharedKey.artwork(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = imageShape,
                    ),
            ) {
                if (type == RowType.TopTen) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(StreamCoreDimens.Spacing.Medium),
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Spacing.Large,
                    vertical = StreamCoreDimens.Spacing.Medium,
                ),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
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
                if (spec.showDescription) {
                    Text(
                        text = content.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = genreText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = content.homeMetadataText(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun RowType.tvCardSpec(): TvCardSpec {
    return when (this) {
        RowType.Featured -> TvCarouselCardSpec
        RowType.ContinueWatching,
        RowType.Landscape -> TvLandscapeCardSpec

        RowType.Poster -> TvPosterCardSpec
        RowType.TopTen -> TvTopTenCardSpec
    }
}

private data class TvCardSpec(
    val width: Dp,
    val aspectRatio: Float,
    val showDescription: Boolean,
)

private val TvCarouselCardSpec = TvCardSpec(
    width = StreamCoreDimens.Tv.Browse.FeaturedCardWidth,
    aspectRatio = StreamCoreDimens.Artwork.LandscapeAspectRatio,
    showDescription = true,
)
private val TvPosterCardSpec = TvCardSpec(
    width = StreamCoreDimens.Tv.Browse.PosterCardWidth,
    aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
    showDescription = false,
)
private val TvLandscapeCardSpec = TvCardSpec(
    width = StreamCoreDimens.Tv.Browse.LandscapeCardWidth,
    aspectRatio = StreamCoreDimens.Artwork.LandscapeAspectRatio,
    showDescription = true,
)
private val TvTopTenCardSpec = TvCardSpec(
    width = StreamCoreDimens.Tv.Browse.TopTenCardWidth,
    aspectRatio = StreamCoreDimens.Artwork.PosterAspectRatio,
    showDescription = false,
)

@PreviewTV
@Composable
private fun TvHomeScreenPreview() {
    StreamCoreTheme {
        TvHomeScreen(
            state = HomeUiState(
                isLoading = false,
                rows = HomePreviewData.rows,
            ),
            onAction = {},
            selectedContentKey = null,
        )
    }
}
