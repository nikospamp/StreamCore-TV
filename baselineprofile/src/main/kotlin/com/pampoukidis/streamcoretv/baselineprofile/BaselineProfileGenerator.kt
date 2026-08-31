package com.pampoukidis.streamcoretv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.pampoukidis.streamcoretv.benchmark.driver.shared.StreamCoreUiDriver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val driver = createDriver(instrumentation)
        driver.ensureProfilePicker(device, TargetPackage)

        baselineProfileRule.collect(
            packageName = TargetPackage,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            driver.awaitProfilePicker(device)
        }
    }

    @Test
    fun measuredJourneys() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val driver = createDriver(instrumentation)
        driver.ensureProfilePicker(device, TargetPackage)

        baselineProfileRule.collect(
            packageName = TargetPackage,
            includeInStartupProfile = false,
        ) {
            pressHome()
            startActivityAndWait()
            driver.collectCriticalUserJourneys(device)
        }
    }

    private fun createDriver(
        instrumentation: android.app.Instrumentation,
    ): StreamCoreUiDriver {
        val arguments = InstrumentationRegistry.getArguments()
        val outputDirectory = File(instrumentation.context.filesDir, "baseline-profile-failures")
        return StreamCoreUiDriver(
            profileName = arguments.getString("profileName", "Nikos"),
            contentTag = arguments.getString("contentTag"),
            failureFile = { name -> File(outputDirectory, name) },
        )
    }

    private companion object {
        const val TargetPackage = "com.pampoukidis.streamcoretv.benchmark"
    }
}
