package com.pampoukidis.streamcoretv.feature.details.web.details

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.web.testing.WebDetailsFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDetailsContractTest {

    @Test
    fun `direct id load does not require an in memory content model`() {
        val action = webDetailsLoadAction(
            profileId = "profile-7",
            contentId = "content-42",
            initialContent = null,
        )

        assertEquals("profile-7", action.request.profileId)
        assertEquals("content-42", action.request.contentId)
        assertNull(action.initialContent)
    }

    @Test
    fun `matching initial content is forwarded as an optional fast path`() {
        val initialContent = WebDetailsFixtures.content
        val action = webDetailsLoadAction(
            profileId = "profile-7",
            contentId = initialContent.id,
            initialContent = initialContent,
        )

        assertEquals(initialContent, action.initialContent)
        assertEquals(initialContent.id, action.request.contentId)
    }

    @Test
    fun `new direct id never renders stale content from the previous details route`() {
        val staleState = WebDetailsFixtures.contentState
        val nextContent = WebDetailsFixtures.content.copy(id = "next-content")

        val withoutInitialContent = staleState.webDetailsDisplayState(
            contentId = nextContent.id,
            initialContent = null,
        )
        val withInitialContent = staleState.webDetailsDisplayState(
            contentId = nextContent.id,
            initialContent = nextContent,
        )

        assertNull(withoutInitialContent.content)
        assertEquals(nextContent, withInitialContent.content)
        assertTrue(withoutInitialContent.isLoading)
        assertTrue(withInitialContent.isLoading)
    }

    @Test
    fun `recommendation focus resolves by exact id`() {
        val state = WebDetailsFixtures.contentState
        val recommendation = state.recommendations[2]
        val key = recommendation.webDetailsRecommendationFocusKey()

        assertEquals(
            WebDetailsFocusTarget(
                sectionKey = WebDetailsRecommendationsSection,
                itemKey = recommendation.id,
                itemIndex = 2,
            ),
            state.findWebDetailsFocusTarget(key),
        )
    }

    @Test
    fun `play focus resolves only while content exists`() {
        val playKey = webDetailsActionFocusKey(WebDetailsPlayItem)

        assertEquals(
            WebDetailsFocusTarget(
                sectionKey = WebDetailsActionsSection,
                itemKey = WebDetailsPlayItem,
            ),
            WebDetailsFixtures.contentState.findWebDetailsFocusTarget(playKey),
        )
        assertNull(DetailsUiState(isLoading = true).findWebDetailsFocusTarget(playKey))
    }

    @Test
    fun `missing and foreign return focus keys do not resolve`() {
        val state = WebDetailsFixtures.contentState

        assertNull(
            state.findWebDetailsFocusTarget(
                WebBrowseFocusKey(
                    destination = WebBrowseDestination.Details,
                    sectionKey = WebDetailsRecommendationsSection,
                    itemKey = "missing",
                ),
            ),
        )
        assertNull(
            state.findWebDetailsFocusTarget(
                WebBrowseFocusKey(
                    destination = WebBrowseDestination.Library,
                    sectionKey = WebDetailsRecommendationsSection,
                    itemKey = state.recommendations.first().id,
                ),
            ),
        )
    }

    @Test
    fun `blank recommendation id cannot create history focus`() {
        assertNull(contentWithId("").webDetailsRecommendationFocusKey())
    }

    private fun contentWithId(id: String): ContentModel {
        return WebDetailsFixtures.content.copy(id = id)
    }
}
