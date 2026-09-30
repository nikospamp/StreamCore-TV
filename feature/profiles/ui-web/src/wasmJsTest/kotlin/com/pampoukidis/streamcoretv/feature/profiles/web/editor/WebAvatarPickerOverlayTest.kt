package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class WebAvatarPickerOverlayTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun directionalKeysMoveFocusWithinTheTrapAndSelectionAndEscapeReachCallbacks(): TestResult {
        val avatars = ProfilesPreviewData.avatars
        val firstTag = ProfilesTestTags.EditorAvatarOptionPrefix + avatars[0].id
        val secondTag = ProfilesTestTags.EditorAvatarOptionPrefix + avatars[1].id
        val fifthTag = ProfilesTestTags.EditorAvatarOptionPrefix + avatars[4].id
        val sixthTag = ProfilesTestTags.EditorAvatarOptionPrefix + avatars[5].id
        var selectedAvatarId: String? = null
        var dismissals = 0
        var visible by mutableStateOf(true)
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.size(24.dp).testTag("outside-avatar-overlay").focusable())
                        if (visible) {
                            WebAvatarPickerOverlay(
                                avatars = avatars,
                                selectedAvatarId = avatars[0].id,
                                onAvatarSelected = { selectedAvatarId = it },
                                onDismissRequest = { dismissals += 1; visible = false },
                            )
                        }
                    }
                }
            }

            onNodeWithTag(firstTag).assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithTag(fifthTag).assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithTag(sixthTag).assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
            onNodeWithTag(secondTag).assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
            onNodeWithTag(firstTag).assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
            onNodeWithTag(firstTag).assertIsFocused()
            onNodeWithTag("outside-avatar-overlay").assertIsNotFocused()
            onNodeWithTag(firstTag).performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithTag(fifthTag).assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
            assertEquals(avatars[4].id, selectedAvatarId)
            onNodeWithTag(fifthTag).performKeyInput { pressKey(Key.Escape) }
            assertEquals(1, dismissals)
            onNodeWithTag(ProfilesTestTags.EditorAvatarDialog).assertDoesNotExist()
        }
    }
}
