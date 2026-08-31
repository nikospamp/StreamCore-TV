package com.pampoukidis.streamcoretv.benchmark.driver.shared

import android.graphics.Rect
import android.os.SystemClock
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File

class StreamCoreUiDriver(
    private val profileName: String,
    private val contentTag: String?,
    private val failureFile: (String) -> File,
    private val timeoutMillis: Long = 10_000L,
) {
    private var searchVisited = false

    fun prepare(device: UiDevice, journey: String) {
        searchVisited = false
        requireAuthenticatedProfile(device)
        if (journey != "profilesToHome") {
            enterHome(device)
        }
        if (journey == "homeToDetails" || journey == "playerToDetails") {
            findContent(device)
        }
        if (journey == "playerToDetails") {
            openDetails(device)
            openPlayer(device)
        }
        SystemClock.sleep(SettleMillis)
    }

    fun navigate(device: UiDevice, journey: String) {
        when (journey) {
            "profilesToHome" -> enterHome(device)
            "homeToDetails" -> openDetails(device)
            "playerToDetails" -> returnFromPlayer(device)
            "initialSearch" -> openSearch(device)
            else -> error("Unknown journey: $journey")
        }
    }

    fun returnToSource(device: UiDevice, journey: String) {
        SystemClock.sleep(SettleMillis)
        when (journey) {
            "profilesToHome" -> returnToProfiles(device)
            "homeToDetails" -> returnToHomeFromDetails(device)
            "playerToDetails" -> openPlayer(device)
            "initialSearch" -> returnToHomeFromSearch(device)
        }
        SystemClock.sleep(SettleMillis)
    }

    /** One deterministic collection covering startup plus every measured navigation path. */
    fun collectCriticalUserJourneys(device: UiDevice) {
        requireAuthenticatedProfile(device)
        enterHome(device)
        exerciseHome(device)

        findContent(device)
        openDetails(device, allowReverseSearch = true)
        returnToHomeFromDetails(device, ensurePinnedContent = false)

        openSearch(device)
        returnToHomeFromSearch(device)

        findContent(device)
        openDetails(device, allowReverseSearch = true)
        openPlayer(device)
        returnFromPlayer(device)
    }

    fun awaitProfilePicker(device: UiDevice) {
        requireAuthenticatedProfile(device)
    }

    fun ensureProfilePicker(device: UiDevice, packageName: String) {
        device.executeShellCommand("am force-stop $packageName")
        device.executeShellCommand("am start -W -n $packageName/com.pampoukidis.streamcoretv.MainActivity")
        if (device.wait(Until.hasObject(By.descStartsWith("$profileName,")), SourceReturnRetryMillis)) {
            device.executeShellCommand("am force-stop $packageName")
            return
        }
        ready(device, "home")
        returnToProfiles(device)
        device.executeShellCommand("am force-stop $packageName")
    }

    fun openPlayer(device: UiDevice) {
        required(device, By.res("details:play")).click()
        ready(device, "player")
        val bounds = required(device, By.res("player:timeline")).visibleBounds
        device.click(bounds.left + bounds.width() / 10, bounds.centerY())
        ready(device, "player")
        SystemClock.sleep(PlayerSettleMillis)
    }

    private fun requireAuthenticatedProfile(device: UiDevice) {
        check(device.wait(Until.hasObject(By.descStartsWith("$profileName,")), timeoutMillis)) {
            "Benchmark app must be authenticated. Open StreamCore Benchmark, sign in, then rerun."
        }
    }

    private fun enterHome(device: UiDevice) {
        required(device, By.descStartsWith("$profileName,")).click()
        ready(device, "home")
    }

    private fun exerciseHome(device: UiDevice) {
        device.findObject(By.res("home:hero"))?.visibleBounds?.let { bounds ->
            val safeBounds = bounds.intersectedWith(device.displayWidth, device.displayHeight)
            if (safeBounds.width() > 0 && safeBounds.height() > 0) {
                device.swipe(
                    safeBounds.right - safeBounds.width() / 6,
                    safeBounds.centerY(),
                    safeBounds.left + safeBounds.width() / 6,
                    safeBounds.centerY(),
                    GestureSteps,
                )
            }
        }
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 4 / 5,
            device.displayWidth / 2,
            device.displayHeight / 3,
            GestureSteps,
        )
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight / 3,
            device.displayWidth / 2,
            device.displayHeight * 4 / 5,
            GestureSteps,
        )
        ready(device, "home")
    }

    private fun openDetails(
        device: UiDevice,
        allowReverseSearch: Boolean = false,
    ) {
        val content = findContent(device, allowReverseSearch)
        SystemClock.sleep(SettleMillis)
        content.click()
        ready(device, "details")
    }

    private fun returnFromPlayer(device: UiDevice) {
        device.pressBack()
        ready(device, "details")
        required(device, By.res("details:play"))
    }

    private fun openSearch(device: UiDevice) {
        required(device, By.text("Search")).click()
        ready(device, "search")
        if (!searchVisited) {
            check(required(device, By.res("search:field")).isFocused) {
                "Initial Search did not request focus"
            }
        }
        searchVisited = true
    }

    private fun returnToProfiles(device: UiDevice) {
        required(device, By.desc("Choose profile")).click()
        if (!device.wait(Until.hasObject(By.descStartsWith("$profileName,")), SourceReturnRetryMillis) &&
            device.hasObject(By.desc("benchmark:home:ready")) &&
            device.hasObject(By.desc("Choose profile"))
        ) {
            required(device, By.desc("Choose profile")).click()
        }
        required(device, By.descStartsWith("$profileName,"))
    }

    private fun returnToHomeFromDetails(
        device: UiDevice,
        ensurePinnedContent: Boolean = true,
    ) {
        device.pressBack()
        ready(device, "home")
        SystemClock.sleep(SettleMillis)
        if (ensurePinnedContent) {
            findContent(device)
        }
    }

    private fun returnToHomeFromSearch(device: UiDevice) {
        device.pressBack()
        required(device, By.res("search:screen"))
        required(device, By.text("Home")).click()
        ready(device, "home")
    }

    private fun findContent(
        device: UiDevice,
        allowReverseSearch: Boolean = false,
    ): UiObject2 {
        val tag = requireNotNull(contentTag) {
            "Supply the exact home:content:<row>:<id> from preflight; never select a moving hero."
        }
        repeat(MaxContentSearchSwipes) {
            device.findObject(By.res(tag))?.let { return it }
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 4 / 5,
                device.displayWidth / 2,
                device.displayHeight / 3,
                GestureSteps,
            )
        }
        if (allowReverseSearch) {
            repeat(MaxContentSearchSwipes * 2) {
                device.findObject(By.res(tag))?.let { return it }
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight / 3,
                    device.displayWidth / 2,
                    device.displayHeight * 4 / 5,
                    GestureSteps,
                )
            }
        }
        error("Pinned content no longer appears on Home: $tag. Do not silently change the dataset.")
    }

    private fun ready(device: UiDevice, screen: String) {
        required(device, By.desc("benchmark:$screen:ready"))
    }

    private fun required(device: UiDevice, selector: BySelector): UiObject2 {
        val result = device.wait(Until.findObject(selector), timeoutMillis)
        if (result == null) {
            val output = failureFile("failure-${SystemClock.elapsedRealtime()}.xml")
            output.parentFile?.mkdirs()
            device.dumpWindowHierarchy(output)
            error("Timed out waiting for $selector; no result is valid. Hierarchy: $output")
        }
        return result
    }

    private fun Rect.intersectedWith(displayWidth: Int, displayHeight: Int): Rect {
        return Rect(this).apply { intersect(0, 0, displayWidth, displayHeight) }
    }

    private companion object {
        const val GestureSteps = 20
        const val MaxContentSearchSwipes = 5
        const val PlayerSettleMillis = 1_000L
        const val SettleMillis = 500L
        const val SourceReturnRetryMillis = 3_000L
    }
}
