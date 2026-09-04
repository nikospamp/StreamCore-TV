package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.ui.unit.dp

internal object WebPlayerTokens {
    val ScreenHorizontalPadding = 48.dp
    val ScreenVerticalPadding = 32.dp
    val TopBarMinHeight = 72.dp
    val ControlStripPadding = 24.dp
    val PrimaryControlMinWidth = 132.dp
    val SecondaryControlMinWidth = 112.dp
    val TimelineHeight = 48.dp
    val FilmstripFrameWidth = 144.dp
    val FilmstripFrameHeight = 81.dp
    val FilmstripFocusedFrameWidth = 176.dp
    val FilmstripFocusedFrameHeight = 99.dp
    val SettingsPanelWidth = 520.dp
    val SettingsPanelMaxHeight = 680.dp
    val SettingsPanelPadding = 32.dp
    val SettingsItemMinHeight = 64.dp
    val ErrorPanelMaxWidth = 520.dp
    val StatusProgressSize = 40.dp
    val StatusPanelMaxWidth = 440.dp
    val SeekFeedbackHorizontalPadding = 120.dp

    const val SurfaceScrimAlpha = 0.74f
    const val SurfaceScrimTransparentAlpha = 0f
    const val SettingsScrimAlpha = 0.62f
    const val PlaceholderAlpha = 0.72f
    const val SelectedContainerAlpha = 0.84f
    const val SeekIntervalMillis = 10_000L
    const val FocusRequestAttempts = 4
}

internal object WebPlayerZOrder {
    const val VideoSurface = 0f
    const val Interaction = 1f
    const val Controls = 2f
    const val Status = 3f
    const val SeekFeedback = 4f
}

internal val WebPlayerSpeedOptions = listOf(
    0.5f,
    0.75f,
    1f,
    1.25f,
    1.5f,
    2f,
)
