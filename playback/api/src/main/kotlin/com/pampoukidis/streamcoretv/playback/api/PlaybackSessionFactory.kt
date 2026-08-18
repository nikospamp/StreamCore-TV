package com.pampoukidis.streamcoretv.playback.api

interface PlaybackSessionFactory {
    fun create(): PlaybackSession
}