package com.pampoukidis.streamcoretv.feature.home.mobile.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
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
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.model.content.imageUrl
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreArtworkSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreContentSharedIdentity
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreTitleSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags

@Composable
fun MobileHomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 16.dp),
        ) {
            HomeHeader(
                isLoading = state.isLoading,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (state.isLoading && state.rows.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
            MobileHomeBody(
                state = state,
                onAction = onAction,
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Featured for you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StreamCoreTextButton(
            text = "Refresh",
            onClick = { onAction(HomeAction.Refresh) },
            enabled = !isLoading,
            modifier = Modifier.testTag(HomeTestTags.RefreshButton),
        )
    }
}

@Composable
private fun MobileHomeBody(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.rows,
                    key = { it.id },
                    contentType = { it.style },
                ) { row ->
                    MobileContentRow(
                        row = row,
                        onAction = onAction,
                        selectedContentKey = selectedContentKey,
                        sharedElementScope = sharedElementScope,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileContentRow(
    row: RowModel,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String?,
    sharedElementScope: StreamCoreSharedElementScope?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.RowPrefix + row.id),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = row.content,
                key = { _, content -> content.id },
                contentType = { _, _ -> row.style },
            ) { index, content ->
                MobileContentCard(
                    rowId = row.id,
                    content = content,
                    style = row.style,
                    rank = index + 1,
                    useSharedTransition = streamCoreContentSharedIdentity(
                        contentId = content.id,
                        row = content.row,
                    ) == selectedContentKey,
                    sharedElementScope = sharedElementScope,
                    onClick = {
                        onAction(HomeAction.ContentSelected(content))
                    },
                )
            }
        }
    }
}

@Composable
private fun MobileContentCard(
    rowId: String,
    content: ContentModel,
    style: RowStyle,
    rank: Int,
    useSharedTransition: Boolean,
    sharedElementScope: StreamCoreSharedElementScope?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = style.mobileCardSpec()
    val imageShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
    val elementScope = if (useSharedTransition) {
        sharedElementScope
    } else {
        null
    }
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { it.name }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .width(spec.width)
            .testTag(HomeTestTags.ContentCardPrefix + rowId + ":" + content.id),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StreamCoreContentImage(
                imageUrl = content.imageUrl(style),
                contentDescription = content.title,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                fallbackTextStyle = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(spec.aspectRatio)
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = streamCoreArtworkSharedKey(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = imageShape,
                    ),
            ) {
                if (style == RowStyle.TopTen) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
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
                if (spec.showDescription) {
                    Text(
                        text = content.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = genreText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = content.homeMetadataText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun RowStyle.mobileCardSpec(): MobileCardSpec {
    return when (this) {
        RowStyle.Carousel -> MobileCarouselCardSpec
        RowStyle.Poster -> MobilePosterCardSpec
        RowStyle.Landscape -> MobileLandscapeCardSpec
        RowStyle.TopTen -> MobileTopTenCardSpec
    }
}

private data class MobileCardSpec(
    val width: Dp,
    val aspectRatio: Float,
    val showDescription: Boolean,
)

private val MobileCarouselCardSpec = MobileCardSpec(
    width = 300.dp,
    aspectRatio = 16f / 9f,
    showDescription = true,
)
private val MobilePosterCardSpec = MobileCardSpec(
    width = 140.dp,
    aspectRatio = 2f / 3f,
    showDescription = false,
)
private val MobileLandscapeCardSpec = MobileCardSpec(
    width = 220.dp,
    aspectRatio = 16f / 9f,
    showDescription = false,
)
private val MobileTopTenCardSpec = MobileCardSpec(
    width = 170.dp,
    aspectRatio = 2f / 3f,
    showDescription = false,
)

@PreviewMobile
@Composable
private fun MobileHomeScreenPreview() {
    StreamCoreTVTheme {
        MobileHomeScreen(
            state = HomeUiState(
                isLoading = false,
                rows = HomePreviewData.rows,
            ),
            onAction = {},
            selectedContentKey = null,
        )
    }
}