package com.pampoukidis.streamcoretv.feature.home.common.home

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.fallbackText
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSharedArtworkImage
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData

/** Artwork and its protective scrims travel together; platforms own bounds and interactions. */
@Composable
fun HomeHeroArtwork(
    content: ContentModel,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    shape: Shape = RectangleShape,
    fallbackTextStyle: TextStyle = MaterialTheme.typography.displayLarge,
) {
    val scrimColor = MaterialTheme.colorScheme.scrim
    val backgroundColor = MaterialTheme.colorScheme.background
    val scrims = remember(scrimColor, backgroundColor) {
        listOf(
            Brush.horizontalGradient(
                colors = listOf(
                    scrimColor.copy(alpha = 0.94f),
                    scrimColor.copy(alpha = 0.62f),
                    scrimColor.copy(alpha = 0.12f),
                ),
            ),
            Brush.verticalGradient(
                0f to scrimColor.copy(alpha = 0f),
                0.5f to scrimColor.copy(alpha = 0f),
                1f to backgroundColor,
            ),
        )
    }
    StreamCoreSharedArtworkImage(
        imageUrl = content.backdrop ?: content.poster,
        contentDescription = content.title,
        fallbackText = content.fallbackText(),
        sharedKey = StreamCoreSharedKey.artwork(contentId = content.id, row = content.row),
        clipShape = shape,
        sharedElementScope = sharedElementScope,
        scrims = scrims,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        fallbackTextStyle = fallbackTextStyle,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HomeHeroArtworkPreview() {
    StreamCoreTheme(darkTheme = true) {
        HomeHeroArtwork(
            content = HomePreviewData.rows.first().content.first(),
            modifier = Modifier.size(
                StreamCoreDimens.Web.Home.HeroCopyMaxWidth,
                StreamCoreDimens.Tv.Browse.HeroHeight,
            ),
        )
    }
}
