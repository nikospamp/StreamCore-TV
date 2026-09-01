package com.pampoukidis.streamcoretv.feature.library.domain

fun interface LibraryClock {
    fun nowEpochMillis(): Long
}
