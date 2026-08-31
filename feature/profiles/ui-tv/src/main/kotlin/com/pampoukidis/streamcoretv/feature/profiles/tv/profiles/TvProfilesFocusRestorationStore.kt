package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

internal data class TvProfilesFocusTarget(
    val profileId: String,
    val profileIndex: Int,
)

internal object TvProfilesFocusRestorationStore {
    private var target: TvProfilesFocusTarget? = null

    fun prepare(profileId: String, profileIndex: Int) {
        target = TvProfilesFocusTarget(
            profileId = profileId,
            profileIndex = profileIndex,
        )
    }

    fun consume(): TvProfilesFocusTarget? {
        val currentTarget = target
        target = null
        return currentTarget
    }

    fun clear() {
        target = null
    }
}
