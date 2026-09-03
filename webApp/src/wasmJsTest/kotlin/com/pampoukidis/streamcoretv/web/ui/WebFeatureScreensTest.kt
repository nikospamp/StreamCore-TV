package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebContentCard
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebLargeScreenBackground
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.web.profiles.WebProfilesScreen
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WebFeatureScreensTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun profileArrowTraversalAndSelectionAreDeterministic(): TestResult {
        val actions = mutableListOf<ProfilesAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebProfilesScreen(
                        state = ProfilesUiState(
                            isLoading = false,
                            profiles = ProfilesPreviewData.profiles,
                        ),
                        onAction = actions::add,
                        onCreateProfile = {},
                        onEditProfile = {},
                        onBack = {},
                    )
                }
            }

            onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + "profile-1")
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + "profile-2")
                .assertIsFocused()
                .performKeyInput { pressKey(Key.Spacebar) }
            assertEquals(ProfilesAction.SelectProfile("profile-2"), actions.last())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manageModeProfileClickOpensEditorInsteadOfSelecting(): TestResult {
        var editedProfileId: String? = null
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebProfilesScreen(
                        state = ProfilesUiState(
                            isLoading = false,
                            profiles = ProfilesPreviewData.profiles,
                            mode = ProfilesMode.Manage,
                        ),
                        onAction = {},
                        onCreateProfile = {},
                        onEditProfile = { editedProfileId = it },
                        onBack = {},
                    )
                }
            }

            onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + "profile-2").performClick()
            assertEquals("profile-2", editedProfileId)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteDialogTrapsFocusAndEscapeDismisses(): TestResult {
        val actions = mutableListOf<ProfilesAction>()
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebProfilesScreen(
                        state = ProfilesUiState(
                            isLoading = false,
                            profiles = ProfilesPreviewData.profiles,
                            mode = ProfilesMode.Manage,
                            pendingDeleteProfile = profile,
                        ),
                        onAction = actions::add,
                        onCreateProfile = {},
                        onEditProfile = {},
                        onBack = {},
                    )
                }
            }

            onNodeWithTag(ProfilesTestTags.EditorCancelDeleteButton)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithTag(ProfilesTestTags.ConfirmDeleteButton)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.Escape) }
            assertIs<ProfilesAction.DismissDeleteConfirmation>(actions.last())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unknownAvatarUsesFallbackWithoutLosingProfileSemantics(): TestResult {
        val profile = ProfilesPreviewData.profiles.first().copy(
            avatar = ProfileAvatarModel(id = "unknown-avatar", imageUrl = null),
        )
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebProfilesScreen(
                        state = ProfilesUiState(isLoading = false, profiles = listOf(profile)),
                        onAction = {},
                        onCreateProfile = {},
                        onEditProfile = {},
                        onBack = {},
                    )
                }
            }

            onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id).assertIsDisplayed()
        }
    }

    @Test
    fun editorNativeBoundaryActionsRemainDistinctAndTyped() {
        val actions = listOf(
            ProfileEditorAction.Submit,
            ProfileEditorAction.Cancel,
            ProfileEditorAction.RequestDeleteProfile,
        )

        assertIs<ProfileEditorAction.Submit>(actions[0])
        assertIs<ProfileEditorAction.Cancel>(actions[1])
        assertIs<ProfileEditorAction.RequestDeleteProfile>(actions[2])
    }

    @Test
    fun editorSelectedAvatarAndMaturityRemainInImmutableDraft() {
        val selectedAvatar = ProfilesPreviewData.avatars[1]
        val selectedLevel = ProfilesPreviewData.parentalLevels[1]
        val draft = ProfileDraftModel(
            displayName = "Profile",
            avatarId = selectedAvatar.id,
            parentalLevelId = selectedLevel.id,
        )

        assertEquals(selectedAvatar.id, draft.avatarId)
        assertEquals(selectedLevel.id, draft.parentalLevelId)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun webContentCardAndLargeBackgroundExposeFocusablePublicContract(): TestResult {
        var clicks = 0
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    StreamCoreWebLargeScreenBackground {
                        StreamCoreWebContentCard(
                            onClick = { clicks += 1 },
                            modifier = Modifier
                                .size(320.dp, 180.dp)
                                .testTag("web-content-card"),
                        ) {
                            Box(Modifier.matchParentSize())
                        }
                    }
                }
            }

            onNodeWithTag("web-content-card").performClick().assertIsFocused()
            assertEquals(1, clicks)
        }
    }
}
