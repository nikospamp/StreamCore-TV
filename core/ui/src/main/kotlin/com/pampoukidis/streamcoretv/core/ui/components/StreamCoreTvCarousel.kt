package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.tv.material3.Carousel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.rememberCarouselState
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamCoreTvCarousel(
    itemCount: Int,
    modifier: Modifier = Modifier,
    initialActiveItemIndex: Int = 0,
    autoScrollDurationMillis: Long = CarouselAutoAdvanceMillis,
    content: @Composable AnimatedContentScope.(index: Int) -> Unit,
) {
    if (itemCount <= 0) return
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val autoAdvanceEnabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) &&
        !LocalInspectionMode.current && itemCount > 1
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    key(itemCount, initialActiveItemIndex) {
        val carouselState = rememberCarouselState(
            initialActiveItemIndex = initialActiveItemIndex.coerceIn(
                minimumValue = 0,
                maximumValue = (itemCount - 1).coerceAtLeast(0),
            ),
        )

        Carousel(
            itemCount = itemCount,
            carouselState = carouselState,
            autoScrollDurationMillis = if (autoAdvanceEnabled) autoScrollDurationMillis else Long.MAX_VALUE,
            contentTransformStartToEnd = fadeIn(
                animationSpec = tween(CarouselTransitionMillis),
            ).togetherWith(
                fadeOut(animationSpec = tween(CarouselTransitionMillis)),
            ),
            contentTransformEndToStart = fadeIn(
                animationSpec = tween(CarouselTransitionMillis),
            ).togetherWith(
                fadeOut(animationSpec = tween(CarouselTransitionMillis)),
            ),
            carouselIndicator = {
                StreamCoreCarouselIndicator(
                    itemCount = itemCount,
                    activeItemIndex = carouselState.activeItemIndex,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(StreamCoreDimens.Spacing.ExtraLarge),
                )
            },
            modifier = modifier.focusProperties {
                // Keep horizontal focus inside until the edge slide. TV Material 1.1's
                // onExit returns a FocusRequester, which the current Unit callback ignores.
                onExit = {
                    val direction = requestedFocusDirection
                    val forward = if (isLtr) FocusDirection.Right else FocusDirection.Left
                    val backward = if (isLtr) FocusDirection.Left else FocusDirection.Right
                    if ((direction == forward && carouselState.activeItemIndex < itemCount - 1) ||
                        (direction == backward && carouselState.activeItemIndex > 0)
                    ) {
                        cancelFocusChange()
                    }
                }
            },
            content = content,
        )
    }
}

@Preview
@Composable
private fun StreamCoreTvCarouselPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreTvCarousel(
            itemCount = 3,
            autoScrollDurationMillis = Long.MAX_VALUE,
            modifier = Modifier.height(StreamCoreDimens.Tv.Browse.HeroHeight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

private const val CarouselTransitionMillis = 200
