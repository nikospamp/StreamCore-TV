package com.pampoukidis.streamcoretv.feature.home.web.home

import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.home.common.home.toHomeContentModel
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebHomeFocusTest {
    private val content = HomePreviewData.rows.toHomeContentModel()
    private val rows = buildList {
        content.continueWatching?.let(::add)
        addAll(content.shelves)
    }

    @Test
    fun contentFocusKeyCarriesHomeDestinationRowAndId() {
        val item = content.featured.first()

        assertEquals(
            WebBrowseFocusKey(
                destination = WebBrowseDestination.Home,
                sectionKey = item.row.orEmpty(),
                itemKey = item.id,
            ),
            item.toWebHomeFocusKey(),
        )
    }

    @Test
    fun contentWithoutRowUsesDeterministicUnsectionedKey() {
        val item = content.featured.first().copy(row = null)

        assertEquals("home:unsectioned", item.toWebHomeFocusKey().sectionKey)
    }

    @Test
    fun resolverFindsExactHeroItem() {
        val key = content.featured[1].toWebHomeFocusKey()

        assertEquals(
            WebHomeFocusTarget(rowIndex = null, contentIndex = 1),
            resolveWebHomeFocusTarget(content, rows, key),
        )
    }

    @Test
    fun resolverFindsExactShelfItemAndDuplicateContentPosition() {
        val rowIndex = rows.indexOfFirst { row -> row.content.size >= 3 }
        val row = rows[rowIndex]
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Home,
            sectionKey = row.id,
            itemKey = row.content[2].id,
        )

        assertEquals(
            WebHomeFocusTarget(rowIndex = rowIndex, contentIndex = 2),
            resolveWebHomeFocusTarget(content, rows, key),
        )
    }

    @Test
    fun resolverRejectsAnotherDestination() {
        val item = content.featured.first()
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Search,
            sectionKey = item.row.orEmpty(),
            itemKey = item.id,
        )

        assertNull(resolveWebHomeFocusTarget(content, rows, key))
    }

    @Test
    fun resolverLeavesMissingItemUnconsumed() {
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Home,
            sectionKey = rows.first().id,
            itemKey = "missing-content",
        )

        assertNull(resolveWebHomeFocusTarget(content, rows, key))
    }

    @Test
    fun matchRequiresWholeHomeKey() {
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Home,
            sectionKey = "featured",
            itemKey = "orbit-fall",
        )

        assertTrue(key.matchesHomeContent("featured", "orbit-fall"))
        assertFalse(key.matchesHomeContent("featured", "other"))
        assertFalse(key.matchesHomeContent("other", "orbit-fall"))
    }
}
