package com.pampoukidis.streamcore.sdk.model.playback

/** Periodic playing sample, an explicit pause/seek/exit checkpoint, or completed playback. */
enum class StreamCorePlaybackProgressEvent {
    Periodic,
    Checkpoint,
    Completed,
}
