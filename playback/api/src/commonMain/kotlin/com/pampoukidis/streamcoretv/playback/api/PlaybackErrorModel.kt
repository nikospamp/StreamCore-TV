package com.pampoukidis.streamcoretv.playback.api

data class PlaybackErrorModel(
    val code: String,
    val message: String,
    val isRecoverable: Boolean,
)