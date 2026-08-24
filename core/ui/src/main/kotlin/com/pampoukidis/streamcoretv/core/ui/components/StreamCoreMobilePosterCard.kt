package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreOverlayDuringSharedTransition
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreMobilePosterCard(
    content: ContentModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContentKey: String? = null,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val useSharedTransition = StreamCoreSharedKey.content(
        contentId = content.id,
        row = content.row,
    ) == selectedContentKey
    val elementScope = sharedElementScope.takeIf { useSharedTransition }
    val shape = MaterialTheme.shapes.medium

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier
            .width(StreamCoreDimens.Mobile.Browse.PosterWidth)
            .semantics(mergeDescendants = true) {}
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
                .clip(shape),
        ) {
            StreamCoreContentImage(
                imageUrl = content.poster,
                contentDescription = null,
                fallbackText = content.fallbackText(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = StreamCoreSharedKey.artwork(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = shape,
                    ),
            )
            StreamCoreMobileArtworkScrim(
                modifier = Modifier.streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = elementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Scrim,
                    clipShape = shape,
                ),
            )
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(StreamCoreDimens.Artwork.ContentPadding)
                    .streamCoreSharedBounds(
                        sharedElementScope = elementScope,
                        key = StreamCoreSharedKey.title(
                            contentId = content.id,
                            row = content.row,
                        ),
                        clipShape = RectangleShape,
                        zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                    ),
            )
        }
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun StreamCoreMobilePosterCardPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreMobilePosterCard(
            content = posterPreviewContent(),
            onClick = {},
        )
    }
}

private fun posterPreviewContent(): ContentModel {
    return ContentModel(
        id = "poster-preview",
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
    )
}
