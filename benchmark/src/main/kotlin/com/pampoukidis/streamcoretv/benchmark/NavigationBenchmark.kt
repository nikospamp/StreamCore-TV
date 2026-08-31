package com.pampoukidis.streamcoretv.benchmark

import android.os.SystemClock
import android.os.Build
import android.os.PowerManager
import android.content.Context
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Each measured block is one navigation journey, never an aggregate tour of the app. */
@RunWith(Parameterized::class)
class NavigationBenchmark(
    private val journey: String,
    private val entry: String,
    private val compilation: String,
) {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    @OptIn(ExperimentalMacrobenchmarkApi::class)
    fun navigation() {
        val arguments = InstrumentationRegistry.getArguments()
        val iterations = arguments.getString("iterations", "10").toInt()
        require(iterations >= 1)
        check(Build.VERSION.SDK_INT >= 34 || compilation == "Ignore") {
            "On Android <14 compilation reset uninstalls the target and deletes authentication/cache. " +
                    "Use explicit Ignore diagnostics, or Android 14+ for the controlled None/Partial matrix."
        }
        val timings = mutableListOf<Double>()
        val thermalStatuses = mutableListOf<Int>()
        val measurementStartThermalStatuses = mutableListOf<Int>()
        val measurementEndThermalStatuses = mutableListOf<Int>()
        val cooldownTimes = mutableListOf<Long>()
        val powerManager = InstrumentationRegistry.getInstrumentation().context
            .getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = BenchmarkTarget.PackageName
        val driver = NavigationDriver(
            profileName = arguments.getString("profileName", "Nikos"),
            contentTag = arguments.getString("contentTag"),
        )
        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(FrameTimingMetric()),
            iterations = iterations,
            compilationMode = when (compilation) {
                "None" -> CompilationMode.None()
                "Ignore" -> CompilationMode.Ignore()
                "Baseline" -> CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Require,
                )

                "Partial" -> CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Disable,
                    warmupIterations = 3,
                )

                else -> error("Unknown compilation mode: $compilation")
            },
            setupBlock = {
                killProcess()
                pressHome()
                val cooldownStarted = SystemClock.elapsedRealtime()
                while (powerManager.currentThermalStatus != PowerManager.THERMAL_STATUS_NONE &&
                    SystemClock.elapsedRealtime() - cooldownStarted < 180_000L
                ) {
                    // Outside the captured interval, with the target stopped. Never
                    // suppress the thermal guard or alter the phone's thermal policy.
                    SystemClock.sleep(10_000)
                }
                val thermalStatus = powerManager.currentThermalStatus
                check(thermalStatus == PowerManager.THERMAL_STATUS_NONE) {
                    "Phone did not cool to thermal status 0 (status=$thermalStatus). Unplug/cool it and resume."
                }
                thermalStatuses += thermalStatus
                cooldownTimes += SystemClock.elapsedRealtime() - cooldownStarted
                device.wakeUp()
                startActivityAndWait()
                driver.prepare(device, journey)
                if (entry == "repeated") {
                    driver.navigate(device, journey)
                    driver.returnToSource(device, journey)
                }
            },
        ) {
            val measurementStartThermalStatus = powerManager.currentThermalStatus
            check(measurementStartThermalStatus == PowerManager.THERMAL_STATUS_NONE) {
                "Phone heated during journey setup (status=$measurementStartThermalStatus). " +
                        "This sample is invalid; cool the phone and retry the batch."
            }
            measurementStartThermalStatuses += measurementStartThermalStatus
            val started = SystemClock.elapsedRealtimeNanos()
            driver.navigate(device, journey)
            timings += (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0
            // Capture the entire 240 ms transition and 150 ms delayed insertions even
            // when the destination's data is already ready. Excluded from readinessWallMs.
            SystemClock.sleep(500)
            val measurementEndThermalStatus = powerManager.currentThermalStatus
            check(measurementEndThermalStatus == PowerManager.THERMAL_STATUS_NONE) {
                "Phone heated during the measured journey (status=$measurementEndThermalStatus). " +
                        "This sample is invalid; cool the phone and retry the batch."
            }
            measurementEndThermalStatuses += measurementEndThermalStatus
        }
        val output = JSONObject()
            .put("journey", journey)
            .put("entry", entry)
            .put("compilation", compilation)
            .put("compilationControlled", compilation != "Ignore")
            .put("iterations", iterations)
            .put("readinessWallMs", JSONArray(timings.takeLast(iterations)))
            .put("thermalStatusBeforeSamples", JSONArray(thermalStatuses.takeLast(iterations)))
            .put("thermalStatusAtMeasurementStart", JSONArray(measurementStartThermalStatuses.takeLast(iterations)))
            .put("thermalStatusAtMeasurementEnd", JSONArray(measurementEndThermalStatuses.takeLast(iterations)))
            .put("cooldownMillisBeforeSamples", JSONArray(cooldownTimes.takeLast(iterations)))
            .put("captureTailMs", 500)
            .put("playerSeekFraction", 0.1)
            .put("contentTag", arguments.getString("contentTag"))
            .put("imageCache", "disk retained; process memory cold; repeated primes the journey once")
            .put("firstEntryDefinition", "first execution of this journey in a restarted process; not first install")
        BenchmarkTarget.outputFile("${journey}_${entry}_${compilation}_journey.json").writeText(output.toString(2))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}_{1}_{2}")
        fun parameters(): List<Array<Any>> {
            val args = InstrumentationRegistry.getArguments()
            val journeys = listOf("profilesToHome", "homeToDetails", "playerToDetails", "initialSearch")
                .filter { args.getString("journey") == null || args.getString("journey") == it }
            val entries = listOf("first", "repeated")
                .filter { args.getString("entry") == null || args.getString("entry") == it }
            // Ignore is opt-in diagnostics only; never part of the controlled matrix.
            val requestedMode = args.getString("compilation")
            val modes = when (requestedMode) {
                "Ignore" -> listOf("Ignore")
                "Baseline" -> listOf("Baseline")
                null -> listOf("None", "Partial")
                else -> listOf("None", "Partial").filter { mode -> requestedMode == mode }
            }
            require(journeys.isNotEmpty() && entries.isNotEmpty() && modes.isNotEmpty())
            return journeys.flatMap { journey ->
                entries.flatMap { entry -> modes.map { mode -> arrayOf<Any>(journey, entry, mode) } }
            }
        }
    }
}
