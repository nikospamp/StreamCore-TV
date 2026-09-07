package com.pampoukidis.streamcoretv.core.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class StreamCoreButtonBaselineTest {
    @get:Rule val rule = createComposeRule()

    @Test fun captureLight() { capture(false) }
    @Test fun captureDark() { capture(true) }

    private fun capture(dark: Boolean) {
        rule.setContent {
            StreamCoreTheme(darkTheme = dark) {
                Surface {
                    Column(Modifier.padding(24.dp)) {
                        OutlinedTextField(value = "viewer@example.com", onValueChange = {})
                        StreamCoreButton("Continue", {}, true)
                        StreamCoreButton("Secondary", {}, true, variant = StreamCoreButtonVariant.Secondary)
                        StreamCoreButton("Compact", {}, true, size = StreamCoreButtonSize.Compact)
                        StreamCoreButton("Disabled", {}, false)
                        StreamCoreButton("Continue", {}, false, loading = true)
                    }
                }
            }
        }
        rule.waitForIdle()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(directory, "buttons-${if (dark) "dark" else "light"}.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
