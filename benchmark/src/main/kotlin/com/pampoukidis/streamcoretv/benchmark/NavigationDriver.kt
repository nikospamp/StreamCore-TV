package com.pampoukidis.streamcoretv.benchmark

import androidx.test.uiautomator.UiDevice
import com.pampoukidis.streamcoretv.benchmark.driver.shared.StreamCoreUiDriver

internal class NavigationDriver(
    private val profileName: String,
    private val contentTag: String?,
) {
    private val delegate = StreamCoreUiDriver(
        profileName = profileName,
        contentTag = contentTag,
        failureFile = BenchmarkTarget::outputFile,
        timeoutMillis = BenchmarkTarget.TimeoutMillis,
    )

    fun prepare(device: UiDevice, journey: String) {
        delegate.prepare(device, journey)
    }

    fun navigate(device: UiDevice, journey: String) {
        delegate.navigate(device, journey)
    }

    fun returnToSource(device: UiDevice, journey: String) {
        delegate.returnToSource(device, journey)
    }

    fun openPlayer(device: UiDevice) {
        delegate.openPlayer(device)
    }
}
