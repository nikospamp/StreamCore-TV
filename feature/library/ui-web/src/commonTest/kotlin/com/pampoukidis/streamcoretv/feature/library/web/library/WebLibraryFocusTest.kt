package com.pampoukidis.streamcoretv.feature.library.web.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class WebLibraryFocusTest {

    @Test
    fun `exact section and item resolve one focus target`() {
        val duplicateInLiked = content(
            id = "shared",
            row = WebLibraryLikedSection,
        )
        val duplicateInMyList = content(
            id = "shared",
            row = WebLibraryMyListSection,
        )
        val state = LibraryUiState(
            isLoading = false,
            likedContent = listOf(duplicateInLiked),
            myListContent = listOf(duplicateInMyList),
        )
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Library,
            sectionKey = WebLibraryMyListSection,
            itemKey = "shared",
        )

        assertEquals(
            WebLibraryFocusTarget(
                sectionKey = WebLibraryMyListSection,
                itemIndex = 0,
            ),
            state.findWebLibraryFocusTarget(key),
        )
    }

    @Test
    fun `missing item and foreign destination do not resolve`() {
        val state = LibraryUiState(
            isLoading = false,
            likedContent = listOf(content("liked", WebLibraryLikedSection)),
        )

        assertNull(
            state.findWebLibraryFocusTarget(
                WebBrowseFocusKey(
                    destination = WebBrowseDestination.Library,
                    sectionKey = WebLibraryLikedSection,
                    itemKey = "missing",
                ),
            ),
        )
        assertNull(
            state.findWebLibraryFocusTarget(
                WebBrowseFocusKey(
                    destination = WebBrowseDestination.Home,
                    sectionKey = WebLibraryLikedSection,
                    itemKey = "liked",
                ),
            ),
        )
    }

    @Test
    fun `first focus target skips empty sections`() {
        val state = LibraryUiState(
            isLoading = false,
            myListContent = listOf(content("saved", WebLibraryMyListSection)),
        )

        assertEquals(
            WebLibraryFocusTarget(
                sectionKey = WebLibraryMyListSection,
                itemIndex = 0,
            ),
            state.firstWebLibraryFocusTarget(),
        )
    }

    @Test
    fun `selection focus key rejects content outside library sections`() {
        assertNull(content("unknown", "home:featured").webLibraryFocusKey())
        assertEquals(
            WebBrowseFocusKey(
                destination = WebBrowseDestination.Library,
                sectionKey = WebLibraryLikedSection,
                itemKey = "liked",
            ),
            content("liked", WebLibraryLikedSection).webLibraryFocusKey(),
        )
    }

    @Test
    fun `profile keyed view model identity isolates profiles`() {
        val first = libraryViewModelKey("profile-a")
        val second = libraryViewModelKey("profile-b")

        assertNotEquals(first, second)
        assertEquals("web-library:profile-a", first)
        assertEquals("web-library:profile-b", second)
    }

    private fun content(
        id: String,
        row: String,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = id,
            description = "",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
            row = row,
        )
    }
}
