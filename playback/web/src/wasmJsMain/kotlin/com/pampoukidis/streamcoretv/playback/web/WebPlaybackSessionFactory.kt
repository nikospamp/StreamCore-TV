package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory

internal class WebPlaybackSessionFactory : PlaybackSessionFactory {
    override fun create(): PlaybackSession {
        return WebPlaybackSession()
    }
}
