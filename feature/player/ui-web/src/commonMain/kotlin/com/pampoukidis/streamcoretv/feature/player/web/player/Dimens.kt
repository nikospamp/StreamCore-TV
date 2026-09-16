package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.ui.unit.dp

internal object WebPlayerTokens {
    val ScreenHorizontalPadding = 48.dp
    val ScreenVerticalPadding = 16.dp
    val ControlIconSize = 28.dp
    val LargeControlSize = 72.dp
    val LargeControlIconSize = 36.dp
    val TimelineHeight = 48.dp
    val FilmstripFrameWidth = 144.dp
    val FilmstripFrameHeight = 81.dp
    val FilmstripFocusedFrameWidth = 176.dp
    val FilmstripFocusedFrameHeight = 99.dp
    val ErrorPanelMaxWidth = 520.dp
    val StatusProgressSize = 40.dp
    val StatusPanelMaxWidth = 440.dp
    val StatusVerticalOffset = (-112).dp
    val SeekFeedbackHorizontalPadding = 120.dp

    const val SurfaceScrimAlpha = 0.74f
    const val SettingsScrimAlpha = 0.46f
    const val SettingsPanelAlpha = 0.995f
    const val SettingsDividerAlpha = 0.5f
    const val SettingsSelectedRowAlpha = 0.72f
    const val PlaceholderAlpha = 0.72f
    const val SeekIntervalMillis = 10_000L
    const val FocusRequestAttempts = 4
}

internal object WebPlayerZOrder {
    const val VideoSurface = 0f
    const val Interaction = 1f
    const val Controls = 2f
    const val Status = 3f
    const val SeekFeedback = 4f
    const val Modal = 5f
}
