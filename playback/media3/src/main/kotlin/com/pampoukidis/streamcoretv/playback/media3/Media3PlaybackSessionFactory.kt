package com.pampoukidis.streamcoretv.playback.media3

import android.content.Context
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory

class Media3PlaybackSessionFactory(
    private val applicationContext: Context,
) : PlaybackSessionFactory {
    override fun create(): PlaybackSession {
        return Media3PlaybackSession(applicationContext)
    }
}