package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.R
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** A single, non-focusable indicator shared by touch pagers and TV carousels. */
@Composable
fun StreamCoreCarouselIndicator(
    itemCount: Int,
    activeItemIndex: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onArtwork,
) {
    if (itemCount <= 1) return

    val selectedIndex = activeItemIndex.coerceIn(0, itemCount - 1)
    val description = stringResource(R.string.carousel_page, selectedIndex + 1, itemCount)
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Indicator.DotSpacing),
        modifier = modifier.semantics { contentDescription = description },
    ) {
        repeat(itemCount) { index ->
            val selected = index == selectedIndex
            val width by animateDpAsState(
                targetValue = if (selected) {
                    StreamCoreDimens.Indicator.SelectedDotWidth
                } else {
                    StreamCoreDimens.Indicator.DotSize
                },
                animationSpec = tween(IndicatorTransitionMillis),
                label = "Carousel marker width",
            )
            val markerColor by animateColorAsState(
                targetValue = color.copy(alpha = if (selected) 1f else 0.4f),
                animationSpec = tween(IndicatorTransitionMillis),
                label = "Carousel marker color",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(StreamCoreDimens.Indicator.DotSize)
                    .clip(CircleShape)
                    .background(markerColor),
            )
        }
    }
}

@Preview
@Composable
private fun StreamCoreCarouselIndicatorPreview() {
    StreamCoreTheme {
        StreamCoreCarouselIndicator(
            itemCount = 5,
            activeItemIndex = 1,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val IndicatorTransitionMillis = 220
