package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileParentalLevelModel
import com.pampoukidis.streamcoretv.feature.profiles.data.EditorRequest
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidateProfileDraftUseCaseTest {

    private val subject = ValidateProfileDraftUseCase()

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
        assertEquals(ProfileFieldError.Blank, result.displayNameError)
        assertEquals(ProfileFieldError.MissingSelection, result.avatarError)
        assertEquals(ProfileFieldError.MissingSelection, result.parentalLevelError)
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

        assertEquals(ProfileFieldError.TooLong, result.displayNameError)
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

        assertEquals(ProfileFieldError.UnknownSelection, result.avatarError)
        assertEquals(ProfileFieldError.UnknownSelection, result.parentalLevelError)
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

    private fun editorOptions(): ProfileEditorOptionsModel {
        return ProfileEditorOptionsModel(
            avatars = listOf(
                ProfileAvatarModel(
                    id = "avatar-1",
                    imageUrl = null,
                ),
            ),
            parentalLevels = listOf(
                ProfileParentalLevelModel(
                    id = "parental-1",
                    label = "Teen",
                    rank = 13,
                ),
            ),
        )
    }
}
