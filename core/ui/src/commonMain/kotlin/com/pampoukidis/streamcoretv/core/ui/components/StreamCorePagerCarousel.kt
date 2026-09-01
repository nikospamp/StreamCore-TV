package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

/** Touch carousel. Pages own their content; the fixed overlay owns the only indicator. */
@Composable
fun StreamCorePagerCarousel(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = StreamCoreDimens.Spacing.Medium,
    indicatorPadding: PaddingValues = PaddingValues(StreamCoreDimens.Spacing.Large),
    indicatorModifier: Modifier = Modifier,
    autoAdvanceEnabled: Boolean = true,
    key: ((Int) -> Any)? = null,
    content: @Composable (Int) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val inspecting = LocalInspectionMode.current
    val dragged by state.interactionSource.collectIsDraggedAsState()
    var pressed by remember { mutableStateOf(false) }
    val itemCount = state.pageCount

    LaunchedEffect(state, lifecycle, autoAdvanceEnabled, inspecting, dragged, pressed, itemCount) {
        if (!autoAdvanceEnabled || inspecting || dragged || pressed || itemCount <= 1) {
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                // Wait for any user fling to settle before starting a fresh reading interval.
                snapshotFlow { state.isScrollInProgress }.first { scrolling -> !scrolling }
                val settledPage = state.settledPage
                delay(CarouselAutoAdvanceMillis)
                if (!state.isScrollInProgress && state.settledPage == settledPage) {
                    state.animateScrollToPage(
                        page = (settledPage + 1) % state.pageCount,
                        animationSpec = tween(CarouselPageTransitionMillis),
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            // Observe without consuming, including presses handled by a child Details button.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                } finally {
                    pressed = false
                }
            }
        },
    ) {
        HorizontalPager(
            state = state,
            contentPadding = contentPadding,
            pageSpacing = pageSpacing,
            key = key,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            content(page)
        }
        StreamCoreCarouselIndicator(
            itemCount = itemCount,
            activeItemIndex = state.currentPage,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(indicatorPadding)
                .then(indicatorModifier),
        )
    }
}

@Preview
@Composable
private fun StreamCorePagerCarouselPreview() {
    StreamCoreTheme {
        StreamCorePagerCarousel(
            state = rememberPagerState { 3 },
            autoAdvanceEnabled = false,
            modifier = Modifier.height(StreamCoreDimens.Carousel.PreviewHeight),
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
    }
}

internal const val CarouselAutoAdvanceMillis = 5_000L
private const val CarouselPageTransitionMillis = 350
