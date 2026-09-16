package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** Drawing shared by browser and TV cards; activation, dimensions, and focus stay in their wrappers. */
@Composable
fun StreamCoreContentCardArtwork(
    title: String,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    progress: PlaybackProgressModel? = null,
    titleModifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        artwork()
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.92f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(StreamCoreDimens.Spacing.Medium),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onArtwork,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = StreamCoreDimens.Spacing.Medium,
                    end = StreamCoreDimens.Spacing.Medium,
                    bottom = if (showProgress) StreamCoreDimens.Spacing.ExtraLarge else StreamCoreDimens.Spacing.Medium,
                )
                .then(titleModifier),
        )
        if (showProgress) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(StreamCoreDimens.Artwork.ProgressHeight)
                    .padding(horizontal = StreamCoreDimens.Artwork.ProgressInset)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.28f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress?.fraction?.coerceIn(0f, 1f) ?: 0f)
                        .height(StreamCoreDimens.Artwork.ProgressHeight)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Preview
@Composable
private fun StreamCoreContentCardArtworkPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreContentCardArtwork(
            title = "The Last Horizon",
            showProgress = false,
            rank = 1,
            modifier = Modifier.size(
                StreamCoreDimens.Tv.Browse.PosterCardWidth,
                StreamCoreDimens.Tv.Browse.PosterCardWidth / StreamCoreDimens.Artwork.PosterAspectRatio,
            ),
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
    }
}
