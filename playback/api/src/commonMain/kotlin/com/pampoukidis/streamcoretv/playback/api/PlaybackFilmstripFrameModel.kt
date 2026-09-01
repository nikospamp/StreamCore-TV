package com.pampoukidis.streamcoretv.playback.api

import androidx.compose.ui.graphics.ImageBitmap

data class PlaybackFilmstripFrameModel(
    val positionMillis: Long,
    val image: ImageBitmap?,
)