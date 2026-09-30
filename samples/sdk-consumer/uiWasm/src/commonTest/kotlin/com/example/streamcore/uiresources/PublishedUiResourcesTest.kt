package com.example.streamcore.uiresources

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PublishedUiResourcesTest {
    @Test
    fun loadsResourcesFromPublishedWasmArtifacts(): TestResult {
        return runTest {
            verifyPublishedUiResources()
        }
    }
}
