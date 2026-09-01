package com.pampoukidis.streamcoretv.feature.library.domain

import kotlin.time.Clock

object SystemLibraryClock : LibraryClock {
    override fun nowEpochMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
