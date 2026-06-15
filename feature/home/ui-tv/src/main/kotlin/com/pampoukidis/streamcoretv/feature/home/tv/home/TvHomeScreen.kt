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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.imageUrl
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = StreamCoreDimens.Tv.ScreenVerticalPadding),
        ) {
            HomeHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                modifier = Modifier.padding(
                    horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding,
                ),
            )
            if (state.isLoading && state.rows.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding),
                )
            }
            TvHomeBody(
                state = state,
                onAction = onAction,
                firstContentFocusRequester = firstContentFocusRequester,
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Tv.ScreenVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(34.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = state.rows,
                    key = { _, row -> row.id },
                    contentType = { _, row -> row.style },
                ) { rowIndex, row ->
                    TvContentRow(
                        row = row,
                        onAction = onAction,
                        firstContentFocusRequester = if (rowIndex == 0) {
                            firstContentFocusRequester
                        } else {
                            null
                        },
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
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding),
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
                horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding,
                vertical = StreamCoreDimens.Tv.FocusBorderPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.style },
            ) { contentIndex, content ->
                TvContentCard(
                    rowId = row.id,
                    content = content,
                    style = row.style,
                    rank = contentIndex + 1,
                    onClick = {
                        onAction(HomeAction.ContentSelected(content))
                    },
                    focusRequester = if (contentIndex == 0) {
                        firstContentFocusRequester
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun TvContentCard(
    rowId: String,
    content: ContentModel,
    style: RowStyle,
    rank: Int,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val spec = style.tvCardSpec()
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { it.name }
    }
    val focusModifier = if (focusRequester == null) {
        Modifier
    } else {
        Modifier.focusRequester(focusRequester)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) 8.dp else 2.dp,
        border = if (isFocused) {
            BorderStroke(
                width = StreamCoreDimens.Tv.FocusBorderWidth,
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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StreamCoreContentImage(
                imageUrl = content.imageUrl(style),
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
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            ) {
                if (style == RowStyle.TopTen) {
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
                            .padding(12.dp),
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    text = "${content.rating}/10 · ${content.pgRatingName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun RowStyle.tvCardSpec(): TvCardSpec {
    return when (this) {
        RowStyle.Carousel -> TvCarouselCardSpec
        RowStyle.Poster -> TvPosterCardSpec
        RowStyle.Landscape -> TvLandscapeCardSpec
        RowStyle.TopTen -> TvTopTenCardSpec
    }
}

private data class TvCardSpec(
    val width: Dp,
    val aspectRatio: Float,
    val showDescription: Boolean,
)

private val TvCarouselCardSpec = TvCardSpec(
    width = 440.dp,
    aspectRatio = 16f / 9f,
    showDescription = true,
)
private val TvPosterCardSpec = TvCardSpec(
    width = 220.dp,
    aspectRatio = 2f / 3f,
    showDescription = false,
)
private val TvLandscapeCardSpec = TvCardSpec(
    width = 320.dp,
    aspectRatio = 16f / 9f,
    showDescription = true,
)
private val TvTopTenCardSpec = TvCardSpec(
    width = 250.dp,
    aspectRatio = 2f / 3f,
    showDescription = false,
)

@PreviewTV
@Composable
private fun TvHomeScreenPreview() {
    StreamCoreTVTheme {
        TvHomeScreen(
            state = HomeUiState(
                isLoading = false,
                rows = HomePreviewData.rows,
            ),
            onAction = {},
        )
    }
}
