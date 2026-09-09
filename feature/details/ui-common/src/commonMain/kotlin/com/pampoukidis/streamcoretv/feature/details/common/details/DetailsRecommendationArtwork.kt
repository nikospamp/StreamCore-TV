package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreContentImage
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData

/** Portrait rendering only; platform callers own sizing, activation, and focus. */
@Composable
fun DetailsRecommendationArtwork(
    content: ContentModel,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.labelSmall,
    ratingStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    Box(
        modifier = modifier
            .aspectRatio(StreamCoreDimens.Artwork.PosterAspectRatio)
            .clip(MaterialTheme.shapes.medium),
    ) {
        StreamCoreContentImage(
            imageUrl = content.poster,
            contentDescription = null,
            fallbackText = content.fallbackText(),
            contentScale = ContentScale.Crop,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fallbackTextStyle = MaterialTheme.typography.displayMedium,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                            MaterialTheme.colorScheme.scrim.copy(alpha = RecommendationScrimAlpha),
                        ),
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(StreamCoreDimens.Artwork.ContentPadding),
        ) {
            Text(
                text = content.title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content.homeMetadataText(),
                style = ratingStyle,
                color = MaterialTheme.colorScheme.onArtwork,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val RecommendationScrimAlpha = 0.78f

@Preview
@Composable
private fun DetailsRecommendationArtworkPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsRecommendationArtwork(
            content = DetailsPreviewData.recommendations.first().copy(poster = ""),
            modifier = Modifier.width(StreamCoreDimens.Mobile.Details.RecommendationCardWidth),
        )
    }
}

@Preview
@Composable
private fun DetailsRecommendationArtworkLongTitlePreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsRecommendationArtwork(
            content = DetailsPreviewData.content.copy(
                title = "The Last Archive Beyond the Northern Horizon",
                poster = "",
                pgRatingName = "Not Rated",
            ),
            modifier = Modifier.width(StreamCoreDimens.Mobile.Details.RecommendationCardWidth),
        )
    }
}
