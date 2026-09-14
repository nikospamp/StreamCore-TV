package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSharedArtworkImage
import com.pampoukidis.streamcoretv.core.ui.extensions.bottomRounded
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementZIndex
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreOverlayDuringSharedTransition
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags

/** Platforms supply hero height, copy insets, and any interactive artwork overlays. */
@Composable
fun DetailsPanoramaHero(
    content: ContentModel,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmall,
    metadataStyle: TextStyle = MaterialTheme.typography.labelMedium,
    contentPadding: PaddingValues = PaddingValues(StreamCoreDimens.Spacing.Large),
    sharedElementScope: StreamCoreSharedElementScope? = null,
    sourceArtworkUrl: String? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val heroShape = MaterialTheme.shapes.bottomRounded
    val scrim = MaterialTheme.colorScheme.scrim
    val artworkUrl = sharedElementScope?.preparedArtworkUrl
        ?: content.backdrop?.takeIf { image -> image.isNotBlank() }
        ?: content.poster
    val artworkScrims = remember(scrim) {
        listOf(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to scrim.copy(alpha = HeroTopScrimAlpha),
                    HeroScrimClearStop to scrim.copy(alpha = 0f),
                    HeroControlsScrimStop to scrim.copy(alpha = HeroControlsScrimAlpha),
                    1f to scrim.copy(alpha = HeroBottomScrimAlpha),
                ),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(heroShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, heroShape)
            .testTag(DetailsTestTags.Hero),
    ) {
        StreamCoreSharedArtworkImage(
            imageUrl = artworkUrl,
            contentDescription = content.title,
            fallbackText = content.fallbackText(),
            sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
            clipShape = heroShape,
            sharedElementScope = sharedElementScope,
            sourceImageUrl = sourceArtworkUrl,
            alignment = Alignment.TopCenter,
            scrims = artworkScrims,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fallbackTextStyle = MaterialTheme.typography.displayLarge,
            // Contrast belongs to the moving picture; text and controls keep their own geometry.
            modifier = Modifier.matchParentSize(),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            Text(
                text = content.title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onArtwork,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = sharedElementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                ),
            )
            Text(
                text = content.heroMetadata(),
                style = metadataStyle,
                color = MaterialTheme.colorScheme.onArtwork.copy(alpha = HeroSupportingContentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.streamCoreOverlayDuringSharedTransition(
                    sharedElementScope = sharedElementScope,
                    zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                ),
            )
            if (bottomContent != null) {
                Spacer(Modifier.height(StreamCoreDimens.Spacing.Medium))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .streamCoreOverlayDuringSharedTransition(
                            sharedElementScope = sharedElementScope,
                            zIndexInOverlay = StreamCoreSharedElementZIndex.Content,
                        ),
                    content = bottomContent,
                )
            }
        }
        overlay()
    }
}

private const val HeroSupportingContentAlpha = 0.84f
private const val HeroTopScrimAlpha = 0.48f
private const val HeroBottomScrimAlpha = 0.96f
private const val HeroScrimClearStop = 0.35f
private const val HeroControlsScrimStop = 0.64f
private const val HeroControlsScrimAlpha = 0.72f

@Preview(widthDp = 840)
@Composable
private fun DetailsPanoramaHeroPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsPanoramaHero(
            content = DetailsPreviewData.content.copy(backdrop = null, poster = ""),
            modifier = Modifier.aspectRatio(StreamCoreDimens.Artwork.LandscapeAspectRatio),
        )
    }
}
