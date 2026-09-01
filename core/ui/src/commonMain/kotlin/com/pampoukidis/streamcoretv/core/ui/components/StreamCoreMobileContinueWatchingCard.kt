package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
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
fun StreamCoreMobileContinueWatchingCard(
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
    val progress = content.playbackProgress?.fraction ?: 0f
    val shape = MaterialTheme.shapes.medium

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier
            .width(StreamCoreDimens.Mobile.Browse.ContinueWatchingWidth)
            .semantics(mergeDescendants = true) {}
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio)
                .clip(shape),
        ) {
            StreamCoreContentImage(
                imageUrl = content.backdrop ?: content.poster,
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = StreamCoreDimens.Artwork.ContentPadding,
                        end = StreamCoreDimens.Artwork.ContentPadding,
                        bottom = StreamCoreDimens.Spacing.Medium,
                    )
                    .streamCoreOverlayDuringSharedTransition(
                        sharedElementScope = elementScope,
                        zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(StreamCoreDimens.Artwork.ProgressHeight)
                    .padding(horizontal = StreamCoreDimens.Artwork.ProgressInset)
                    .streamCoreOverlayDuringSharedTransition(
                        sharedElementScope = elementScope,
                        zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.24f))
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = progress,
                            range = 0f..1f,
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(StreamCoreDimens.Artwork.ProgressHeight)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
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
private fun StreamCoreMobileContinueWatchingCardPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreMobileContinueWatchingCard(
            content = continueWatchingPreviewContent(),
            onClick = {},
        )
    }
}

private fun continueWatchingPreviewContent(): ContentModel {
    return ContentModel(
        id = "continue-watching-preview",
        title = "Northern Signal",
        description = "",
        rating = 9,
        pgRatingName = "TV-14",
        pgRatingLevel = 14,
        poster = "",
        backdrop = null,
        cast = emptyList(),
        releaseDate = 0L,
        genres = emptyList(),
        playbackProgress = PlaybackProgressModel(
            positionMillis = 42L,
            durationMillis = 100L,
        ),
    )
}
