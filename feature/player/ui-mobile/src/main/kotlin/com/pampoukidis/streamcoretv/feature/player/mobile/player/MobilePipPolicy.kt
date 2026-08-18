package com.pampoukidis.streamcoretv.feature.player.mobile.player

import android.os.Build

internal object MobilePipPolicy {
    fun isSupported(apiLevel: Int, hasSystemFeature: Boolean): Boolean {
        return apiLevel >= Build.VERSION_CODES.O && hasSystemFeature
    }

    fun supportsExplicitEntry(apiLevel: Int): Boolean {
        return apiLevel >= Build.VERSION_CODES.O
    }

    fun shouldEnableAutoEnter(apiLevel: Int, isPlaying: Boolean): Boolean {
        return apiLevel >= Build.VERSION_CODES.S && isPlaying
    }

    fun isForegroundOnStop(isInPictureInPicture: Boolean): Boolean {
        return isInPictureInPicture
    }
}