package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.ui.Modifier

internal expect fun Modifier.webPlayerPointerInteraction(
    onInteraction: () -> Unit,
): Modifier
