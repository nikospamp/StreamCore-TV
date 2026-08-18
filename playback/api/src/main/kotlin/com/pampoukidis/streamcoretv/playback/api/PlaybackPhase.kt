package com.pampoukidis.streamcoretv.playback.api

enum class PlaybackPhase {
    Idle,
    Preparing,
    Ready,
    Buffering,
    Ended,
    Error,
}