package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test

class WebSelectorProbeTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun stableTestTagsDriveClickFocusAndTextInput(): TestResult {
        return runComposeUiTest {
            setContent {
                WebSelectorProbe()
            }

            onNodeWithTag(WebSelectorProbeTags.FocusButton).performClick()
            onNodeWithTag(WebSelectorProbeTags.Input)
                .assertIsFocused()
                .performTextInput("wasm")
            onNodeWithTag(WebSelectorProbeTags.Status).assertTextContains("Input value: wasm")
        }
    }
}
