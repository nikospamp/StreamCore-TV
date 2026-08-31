package com.pampoukidis.streamcoretv.benchmark

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** Authentication is deliberately manual; this driver never copies a production session. */
@RunWith(AndroidJUnit4::class)
class BenchmarkPreflight {
    @Test
    fun authenticatedHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val profileName = InstrumentationRegistry.getArguments().getString("profileName", "Nikos")
        device.executeShellCommand("am force-stop ${BenchmarkTarget.PackageName}")
        device.executeShellCommand("am start -W -n ${BenchmarkTarget.PackageName}/com.pampoukidis.streamcoretv.MainActivity")
        check(device.wait(Until.hasObject(By.descStartsWith("$profileName,")), BenchmarkTarget.TimeoutMillis)) {
            "Sign in to the separate StreamCore Benchmark installation before measurement."
        }
        checkNotNull(device.findObject(By.descStartsWith("$profileName,"))) { "Missing benchmark profile: $profileName" }.click()
        check(device.wait(Until.hasObject(By.desc("benchmark:home:ready")), BenchmarkTarget.TimeoutMillis))
        val tags = device.findObjects(By.res(Pattern.compile("home:content:.*")))
            .map { it.resourceName }.distinct()
        val result = JSONObject()
            .put("model", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("sdk", Build.VERSION.SDK_INT)
            .put("fingerprint", Build.FINGERPRINT)
            .put("contentTags", JSONArray(tags))
            .put("thermal", device.executeShellCommand("dumpsys thermalservice"))
            .put("display", device.executeShellCommand("dumpsys display"))
            .put("battery", device.executeShellCommand("dumpsys battery"))
            .put("animations", device.executeShellCommand("settings get global animator_duration_scale"))
        BenchmarkTarget.outputFile("preflight.json").writeText(result.toString(2))
        device.dumpWindowHierarchy(BenchmarkTarget.outputFile("home.xml"))
        check(tags.isNotEmpty()) { "No stable Home content card is visible; inspect home.xml before running." }
    }
}
