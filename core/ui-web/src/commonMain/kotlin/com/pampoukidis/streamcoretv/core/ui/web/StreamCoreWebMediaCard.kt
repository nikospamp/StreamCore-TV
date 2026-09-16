package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.model.content.imageUrl
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentCardArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSharedArtworkImage
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** TV artwork composition with browser activation and focus; all data remains provider-neutral. */
@Composable
fun StreamCoreWebMediaCard(
    content: ContentModel,
    type: RowType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    rank: Int? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val width = when (type) {
        RowType.Featured -> StreamCoreDimens.Tv.Browse.FeaturedCardWidth
        RowType.Poster -> StreamCoreDimens.Tv.Browse.PosterCardWidth
        RowType.TopTen -> StreamCoreDimens.Tv.Browse.TopTenCardWidth
        RowType.ContinueWatching, RowType.Landscape -> StreamCoreDimens.Tv.Browse.LandscapeCardWidth
    }
    val aspectRatio = when (type) {
        RowType.Poster, RowType.TopTen -> StreamCoreDimens.Artwork.PosterAspectRatio
        else -> StreamCoreDimens.Artwork.LandscapeAspectRatio
    }
    val shape = MaterialTheme.shapes.large
    val scrim = MaterialTheme.colorScheme.scrim
    val scrims = remember(scrim) {
        listOf(Brush.verticalGradient(listOf(scrim.copy(alpha = 0f), scrim.copy(alpha = 0.82f))))
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier.width(width).onFocusChanged { focused = it.hasFocus },
    ) {
        StreamCoreWebActionSurface(
            onClick = onClick,
            selected = selected,
            shape = shape,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
        ) {
            StreamCoreContentCardArtwork(
                title = content.title,
                showProgress = type == RowType.ContinueWatching,
                rank = rank.takeIf { type == RowType.TopTen },
                progress = content.playbackProgress,
                modifier = Modifier.fillMaxSize(),
            ) {
                StreamCoreSharedArtworkImage(
                    imageUrl = content.imageUrl(type),
                    contentDescription = content.title,
                    fallbackText = content.fallbackText(),
                    sharedKey = StreamCoreSharedKey.artwork(content.id, content.row),
                    clipShape = shape,
                    scrims = scrims,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = content.homeMetadataText(),
            style = MaterialTheme.typography.labelMedium,
            color = if (focused) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun StreamCoreWebMediaCardPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebMediaCard(
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
        )
    }
}
