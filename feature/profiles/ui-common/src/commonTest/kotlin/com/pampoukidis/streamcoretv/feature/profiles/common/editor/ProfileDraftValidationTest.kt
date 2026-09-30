package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileAvatar
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileParentalLevel
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.api.validation.ProfileValidator
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileValidationResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileFieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileDraftValidationTest {

    private fun subject(draft: ProfileDraftModel, options: StreamCoreProfileEditorOptions?): StreamCoreProfileValidationResult {
        return ProfileValidator.validate(StreamCoreCreateProfile(draft.displayName, draft.avatarId, draft.parentalLevelId), options)
    }

    @Test
    fun `valid draft accepts trimmed display name and known selections`() {
        val result = subject(
            draft = ProfileDraftModel(
                displayName = "  Viewer  ",
                avatarId = "avatar-1",
                parentalLevelId = "parental-1",
            ),
            options = editorOptions(),
        )

        assertTrue(result.isValid)
        assertNull(result.displayNameError)
        assertNull(result.avatarError)
        assertNull(result.parentalLevelError)
    }

    @Test
    fun `blank fields require a name and selections`() {
        val result = subject(
            draft = ProfileDraftModel(
                displayName = " \t ",
                avatarId = "",
                parentalLevelId = "",
            ),
            options = editorOptions(),
        )

        assertFalse(result.isValid)
        assertEquals(StreamCoreProfileFieldError.Blank, result.displayNameError)
        assertEquals(StreamCoreProfileFieldError.MissingSelection, result.avatarError)
        assertEquals(StreamCoreProfileFieldError.MissingSelection, result.parentalLevelError)
    }

    @Test
    fun `display name longer than 32 characters is rejected after trimming`() {
        val result = subject(
            draft = ProfileDraftModel(
                displayName = "  ${"a".repeat(33)}  ",
                avatarId = "avatar-1",
                parentalLevelId = "parental-1",
            ),
            options = editorOptions(),
        )

        assertEquals(StreamCoreProfileFieldError.TooLong, result.displayNameError)
    }

    @Test
    fun `unknown option identifiers are rejected when options are loaded`() {
        val result = subject(
            draft = ProfileDraftModel(
                displayName = "Viewer",
                avatarId = "unknown-avatar",
                parentalLevelId = "unknown-parental",
            ),
            options = editorOptions(),
        )

        assertEquals(StreamCoreProfileFieldError.UnknownSelection, result.avatarError)
        assertEquals(StreamCoreProfileFieldError.UnknownSelection, result.parentalLevelError)
    }

    @Test
    fun `editor requests preserve create and edit mode identity`() {
        val createRequest = EditorRequest(
            mode = ProfileEditorMode.Create,
            profileId = null,
        )
        val editRequest = EditorRequest(
            mode = ProfileEditorMode.Edit,
            profileId = "profile-1",
        )

        assertEquals(ProfileEditorMode.Create, createRequest.mode)
        assertNull(createRequest.profileId)
        assertEquals(ProfileEditorMode.Edit, editRequest.mode)
        assertEquals("profile-1", editRequest.profileId)
    }

    private fun editorOptions(): StreamCoreProfileEditorOptions {
        return StreamCoreProfileEditorOptions(
            avatars = listOf(
                StreamCoreProfileAvatar(
                    id = "avatar-1",
                    imageUrl = null,
                ),
            ),
            parentalLevels = listOf(
                StreamCoreProfileParentalLevel(
                    id = "parental-1",
                    label = "Teen",
                    rank = 13,
                ),
            ),
        )
    }
}
