package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.model.content.imageUrl
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV

@Composable
fun StreamCoreTvContentCard(
    content: ContentModel,
    type: RowType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    focusRequester: FocusRequester? = null,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val spec = type.tvCardSpec()
    val imageShape = MaterialTheme.shapes.large
    val imageUrl = content.imageUrl(type)
    val scrimColor = MaterialTheme.colorScheme.scrim
    val artworkScrims = remember(scrimColor) {
        listOf(
            Brush.verticalGradient(
                colors = listOf(scrimColor.copy(alpha = 0f), scrimColor.copy(alpha = 0.82f)),
            ),
        )
    }
    val contentKey = StreamCoreSharedKey.content(
        contentId = content.id,
        row = content.row,
    )
    val elementScope = sharedElementScope.takeIf { contentKey == selectedContentKey }
    val focusModifier = if (focusRequester == null) {
        Modifier
    } else {
        Modifier.focusRequester(focusRequester)
    }

    Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            StreamCoreDimens.Spacing.Small,
        ),
        modifier = modifier
            .width(spec.width)
            .then(focusModifier)
            .onFocusChanged { focusState -> isFocused = focusState.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Surface(
            shape = imageShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            tonalElevation = 0.dp,
            border = if (isFocused) {
                BorderStroke(
                    width = StreamCoreDimens.Tv.Focus.BorderWidth,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(spec.height),
        ) {
            StreamCoreContentCardArtwork(
                title = content.title,
                showProgress = type == RowType.ContinueWatching,
                rank = rank.takeIf { type == RowType.TopTen },
                progress = content.playbackProgress,
                titleModifier = Modifier.streamCoreSharedBounds(
                    sharedElementScope = elementScope,
                    key = StreamCoreSharedKey.title(contentId = content.id, row = content.row),
                    clipShape = RectangleShape,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                ),
            ) {
                StreamCoreSharedArtworkImage(
                    imageUrl = imageUrl,
                    contentDescription = content.title,
                    fallbackText = content.fallbackText(),
                    sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
                    clipShape = imageShape,
                    sharedElementScope = elementScope,
                    scrims = artworkScrims,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    fallbackTextStyle = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun RowType.tvCardSpec(): TvCardSpec {
    return when (this) {
        RowType.Featured -> TvCardSpec(
            width = StreamCoreDimens.Tv.Browse.FeaturedCardWidth,
            height = StreamCoreDimens.Tv.Browse.FeaturedCardWidth /
                    StreamCoreDimens.Artwork.LandscapeAspectRatio,
        )

        RowType.ContinueWatching,
        RowType.Landscape -> TvCardSpec(
            width = StreamCoreDimens.Tv.Browse.LandscapeCardWidth,
            height = StreamCoreDimens.Tv.Browse.LandscapeCardWidth /
                    StreamCoreDimens.Artwork.LandscapeAspectRatio,
        )

        RowType.Poster -> TvCardSpec(
            width = StreamCoreDimens.Tv.Browse.PosterCardWidth,
            height = StreamCoreDimens.Tv.Browse.PosterCardWidth /
                    StreamCoreDimens.Artwork.PosterAspectRatio,
        )

        RowType.TopTen -> TvCardSpec(
            width = StreamCoreDimens.Tv.Browse.TopTenCardWidth,
            height = StreamCoreDimens.Tv.Browse.TopTenCardWidth /
                    StreamCoreDimens.Artwork.PosterAspectRatio,
        )
    }
}

private data class TvCardSpec(
    val width: Dp,
    val height: Dp,
)

@PreviewTV
@Composable
private fun StreamCoreTvContentCardPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreamCoreTvContentCard(
                content = ContentModel(
                    id = "preview",
                    title = "The Last Horizon",
                    description = "",
                    rating = 8,
                    pgRatingName = "PG-13",
                    pgRatingLevel = 13,
                    poster = "",
                    backdrop = null,
                    cast = emptyList(),
                    releaseDate = 0L,
                    genres = emptyList(),
                    row = "preview",
                ),
                type = RowType.Poster,
                onClick = {},
                modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
            )
        }
    }
}
