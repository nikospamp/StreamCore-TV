package com.pampoukidis.streamcoretv.feature.player.common.player

import kotlin.time.Clock

object SystemPlayerClock : PlayerClock {
    override fun nowEpochMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
