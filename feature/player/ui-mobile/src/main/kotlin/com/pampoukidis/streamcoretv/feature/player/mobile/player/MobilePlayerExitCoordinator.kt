package com.pampoukidis.streamcoretv.feature.player.mobile.player

import android.content.res.Configuration

internal class MobilePlayerWindowSession(
    val sourceConfigurationOrientation: Int,
    private val previousRequestedOrientation: Int,
    private val requestOrientation: (Int) -> Unit,
) {
    private var orientationRestored = false

    fun restoreOrientation(): Boolean {
        if (orientationRestored) {
            return false
        }
        orientationRestored = true
        requestOrientation(previousRequestedOrientation)
        return true
    }
}

internal fun requiresOrientationSettlement(
    sourceOrientation: Int?,
    currentOrientation: Int,
): Boolean {
    return sourceOrientation != null &&
            sourceOrientation != Configuration.ORIENTATION_UNDEFINED &&
            sourceOrientation != currentOrientation
}

internal class MobilePlayerExitCoordinator(
    private val disableAutoEnterPip: () -> Unit,
    private val restoreOrientation: () -> Unit,
    private val onExitStarted: () -> Unit,
    private val navigateBack: () -> Unit,
) {
    var isExitPending: Boolean = false
        private set

    private var navigationCompleted = false

    fun requestExit(): Boolean {
        if (isExitPending || navigationCompleted) {
            return false
        }
        isExitPending = true
        disableAutoEnterPip()
        restoreOrientation()
        onExitStarted()
        return true
    }

    fun configurationSettled() {
        navigateOnce()
    }

    fun timeout() {
        navigateOnce()
    }

    fun cancellationFallback() {
        navigateOnce()
    }

    private fun navigateOnce() {
        if (!isExitPending || navigationCompleted) {
            return
        }
        navigationCompleted = true
        isExitPending = false
        navigateBack()
    }
}
