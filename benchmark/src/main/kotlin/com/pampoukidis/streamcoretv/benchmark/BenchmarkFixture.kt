package com.pampoukidis.streamcoretv.benchmark

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/** Changes only the isolated benchmark installation, through the same UI as a user. */
@RunWith(AndroidJUnit4::class)
class BenchmarkFixture {
    @Test
    fun seedAndCheckCleanup() {
        val args = InstrumentationRegistry.getArguments()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val driver = NavigationDriver(args.getString("profileName", "Nikos"), args.getString("contentTag"))
        device.executeShellCommand("am force-stop ${BenchmarkTarget.PackageName}")
        device.executeShellCommand("am start -W -n ${BenchmarkTarget.PackageName}/com.pampoukidis.streamcoretv.MainActivity")
        driver.prepare(device, "playerToDetails")
        val playingMemory = device.executeShellCommand("dumpsys meminfo ${BenchmarkTarget.PackageName}")
        driver.navigate(device, "playerToDetails")
        listOf("details:like", "details:my-list").forEach { tag ->
            val action = checkNotNull(device.wait(Until.findObject(By.res(tag)), BenchmarkTarget.TimeoutMillis))
            if (!action.isChecked) action.click()
            check(device.wait(Until.hasObject(By.res(tag).checked(true).enabled(true)), BenchmarkTarget.TimeoutMillis))
        }
        val memoryAfterReturns = JSONArray()
        repeat(args.getString("cleanupIterations", "3").toInt()) {
            driver.openPlayer(device)
            driver.navigate(device, "playerToDetails")
            SystemClock.sleep(1_000)
            memoryAfterReturns.put(device.executeShellCommand("dumpsys meminfo ${BenchmarkTarget.PackageName}"))
        }
        // Prime the same disk/shader/UI paths before either build's measurements.
        // CompilationMode will reset/warm ART separately after this fixture step.
        driver.returnToSource(device, "homeToDetails")
        driver.navigate(device, "initialSearch")
        driver.returnToSource(device, "initialSearch")
        BenchmarkTarget.outputFile("fixture.json").writeText(
            JSONObject()
                .put("contentTag", args.getString("contentTag"))
                .put("profileName", args.getString("profileName", "Nikos"))
                .put("seekFraction", 0.1)
                .put("liked", true)
                .put("inMyList", true)
                .put("searchPrimed", true)
                .put("memoryWhilePlaying", playingMemory)
                .put("memoryAfterPlayerReturns", memoryAfterReturns)
                .put("warning", "Memory snapshots screen for retained allocations; they do not prove an object leak or absence of leaks.")
                .toString(2),
        )
    }
}
