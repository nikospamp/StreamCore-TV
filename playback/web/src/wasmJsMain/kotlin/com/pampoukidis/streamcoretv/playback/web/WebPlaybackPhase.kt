package com.pampoukidis.streamcoretv.playback.web

internal enum class WebPlaybackPhase {
    Idle,
    Preparing,
    Buffering,
    Ready,
    Ended,
    Error,
}
