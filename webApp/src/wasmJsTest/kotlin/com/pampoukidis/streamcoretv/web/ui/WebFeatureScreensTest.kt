package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.data.LoginFieldError
import com.pampoukidis.streamcoretv.feature.login.web.login.WebLoginScreen
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.web.editor.WebProfileEditorScreen
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.web.profiles.WebProfilesScreen
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebFeatureScreensTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginStartsOnIdentifierAndArrowDownMovesToPassword(): TestResult {
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebLoginScreen(state = LoginUiState(), onAction = {})
                }
            }

            onNodeWithTag(LoginTestTags.IdentifierField)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithTag(LoginTestTags.PasswordField).assertIsFocused()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginValidationErrorsRemainSemanticAndPasswordIsMasked(): TestResult {
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebLoginScreen(
                        state = LoginUiState(
                            identifierError = LoginFieldError.Required,
                            passwordError = LoginFieldError.Required,
                        ),
                        onAction = {},
                    )
                }
            }

            onNodeWithTag(LoginTestTags.IdentifierField).assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Error),
            )
            onNodeWithTag(LoginTestTags.PasswordField).assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Error),
            )
            onNodeWithTag(LoginTestTags.PasswordField).assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Password),
            )
        }
    }

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
                .performClick()
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editorKeyboardInputAndPointerOptionsEmitSharedActions(): TestResult {
        val actions = mutableListOf<ProfileEditorAction>()
        val defaultAvatar = ProfilesPreviewData.avatars.first()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebProfileEditorScreen(
                        state = ProfileEditorScreenUiState(
                            mode = ProfileEditorMode.Create,
                            isLoading = false,
                            editorOptions = ProfilesPreviewData.editorOptions.copy(
                                avatars = ProfilesPreviewData.avatars.take(3),
                            ),
                            editor = ProfileEditorFormUiState(
                                mode = ProfileEditorMode.Create,
                                draft = ProfileDraftModel(
                                    avatarId = defaultAvatar.id,
                                    parentalLevelId = ProfilesPreviewData.parentalLevels.first().id,
                                ),
                            ),
                        ),
                        onAction = actions::add,
                    )
                }
            }

            onNodeWithTag(ProfilesTestTags.EditorDisplayNameField)
                .assertIsFocused()
                .performTextInput("Web profile")
            onNodeWithTag(
                ProfilesTestTags.EditorAvatarOptionPrefix + ProfilesPreviewData.avatars[1].id,
            ).performClick()
            onNodeWithTag(ProfilesTestTags.EditorSubmitButton).performClick()

            assertTrue(actions.contains(ProfileEditorAction.DisplayNameChanged("Web profile")))
            assertTrue(
                actions.contains(ProfileEditorAction.AvatarChanged(ProfilesPreviewData.avatars[1].id)),
            )
            assertTrue(actions.any { it is ProfileEditorAction.Submit })
        }
    }
}
