package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pampoukidis.streamcoretv.core.ui.theme.LocalStreamCoreControlStyle
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebControlStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StreamCoreControlStyleTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun overrideReachesTouchTvWebComposeAndCss() {
        rule.setContent {
            StreamCoreTheme(darkTheme = true) {
                val style = StreamCoreControlDefaults.style().copy(
                    primary = Color.Magenta,
                    onPrimary = Color.Black,
                    disabledContainer = Color.Cyan,
                    disabledContent = Color.Black,
                    buttonRadius = 4.dp,
                    buttonLabel = StreamCoreControlDefaults.style().buttonLabel.copy(fontSize = 20.sp),
                )
                val css = StreamCoreWebControlStyle(style)
                assertTrue(css.button().contains("rgba(255,0,255,1.0)"))
                assertTrue(css.button().contains("border-radius:4.0px"))
                assertTrue(css.button().contains("font-size:1.25rem"))
                assertTrue(css.button(enabled = false).contains("rgba(0,255,255,1.0)"))
                CompositionLocalProvider(LocalStreamCoreControlStyle provides style) {
                    Surface {
                        Column {
                            StreamCoreButton("Touch", {}, true, Modifier.size(180.dp, 60.dp).testTag("touch"))
                            StreamCoreTvButton("TV", {}, true, Modifier.size(180.dp, 60.dp).testTag("tv"))
                            StreamCoreWebButton("Web", {}, Modifier.size(180.dp, 60.dp).testTag("web"))
                            StreamCoreButton("Disabled", {}, false, Modifier.size(180.dp, 60.dp).testTag("disabled"))
                            StreamCoreButton("Touch loading", {}, true, Modifier.size(180.dp, 60.dp).testTag("touch-loading"), loading = true)
                            StreamCoreTvButton("TV loading", {}, true, Modifier.size(180.dp, 60.dp).testTag("tv-loading"), loading = true)
                            StreamCoreWebButton("Web loading", {}, Modifier.size(180.dp, 60.dp).testTag("web-loading"), loading = true)
                        }
                    }
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)
        for (tag in listOf("touch", "tv", "web")) {
            val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
            // Above the text and inside the overridden small corner: verifies actual renderer fill.
            assertEquals(Color.Magenta, pixels[12, 12])
        }
        assertEquals(Color.Cyan, rule.onNodeWithTag("disabled").captureToImage().toPixelMap()[12, 12])
        for (tag in listOf("touch-loading", "tv-loading", "web-loading")) {
            val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
            assertEquals(Color.Cyan, pixels[12, 12])
            val hasContentColoredSpinner = (0 until pixels.height).any { y ->
                (0 until pixels.width).any { x ->
                    val pixel = pixels[x, y]
                    pixel.red < 0.2f && pixel.green < 0.2f && pixel.blue < 0.2f
                }
            }
            assertTrue("$tag must use the disabled content color for its spinner", hasContentColoredSpinner)
        }
    }

    @Test
    fun loadingRetainsActionNamesAndBlocksActivationAcrossRenderers() {
        var clicks = 0
        rule.setContent {
            StreamCoreTheme {
                Column {
                    StreamCoreButton("Touch action", { clicks++ }, true, loading = true)
                    StreamCoreTvButton("TV action", { clicks++ }, true, loading = true)
                    StreamCoreWebButton("Web action", { clicks++ }, loading = true)
                }
            }
        }
        for (label in listOf("Touch action", "TV action", "Web action")) {
            rule.onNodeWithContentDescription(label).assertIsNotEnabled().performClick()
        }
        assertEquals(0, clicks)
    }

    @Test
    fun tvFocusAndPressUseSharedRoles() {
        rule.setContent {
            StreamCoreTheme(darkTheme = true) {
                assertEquals(
                    androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    androidx.tv.material3.MaterialTheme.colorScheme.primary,
                )
                val style = StreamCoreControlDefaults.style().copy(
                    primary = Color.Magenta,
                    pressed = Color.Yellow,
                    buttonRadius = 4.dp,
                )
                CompositionLocalProvider(LocalStreamCoreControlStyle provides style) {
                    StreamCoreTvButton("Press", {}, true, Modifier.size(180.dp, 60.dp).testTag("press"))
                }
            }
        }
        rule.onNodeWithTag("press").performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag("press").assertIsFocused()
        val focused = rule.onNodeWithTag("press").captureToImage().toPixelMap()
        assertEquals(Color.Magenta, focused[focused.width / 2, focused.height / 4])
        rule.onNodeWithTag("press").performKeyInput { keyDown(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(250)
        val pressed = rule.onNodeWithTag("press").captureToImage().toPixelMap()
        assertEquals(Color.Yellow, pressed[pressed.width / 2, pressed.height / 4])
        rule.onNodeWithTag("press").performKeyInput { keyUp(Key.DirectionCenter) }
    }

    @Test
    fun tvDpadFocusAndActivationSurviveThemeMapping() {
        var clicks = 0
        rule.setContent {
            StreamCoreTheme(darkTheme = true) {
                Column {
                    StreamCoreTvButton("First", { clicks++ }, true)
                    StreamCoreTvButton("Second", { clicks++ }, true)
                }
            }
        }
        rule.onNodeWithText("First").performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithText("First").assertIsFocused()
        rule.onNodeWithText("First").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("Second").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, clicks)
    }
}
