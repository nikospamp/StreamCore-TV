package com.pampoukidis.streamcoretv.feature.home.web.home

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeContentModel

internal fun ContentModel.toWebHomeFocusKey(): WebBrowseFocusKey {
    return WebBrowseFocusKey(
        destination = WebBrowseDestination.Home,
        sectionKey = row?.takeIf(String::isNotBlank) ?: UnsectionedHomeSectionKey,
        itemKey = id,
    )
}

internal fun resolveWebHomeFocusTarget(
    content: HomeContentModel,
    rows: List<RowModel>,
    key: WebBrowseFocusKey?,
): WebHomeFocusTarget? {
    if (key?.destination != WebBrowseDestination.Home) {
        return null
    }

    val heroIndex = content.featured.indexOfFirst { item ->
        item.matches(key)
    }
    if (heroIndex >= 0) {
        return WebHomeFocusTarget(
            rowIndex = null,
            contentIndex = heroIndex,
        )
    }

    rows.forEachIndexed { rowIndex, row ->
        val contentIndex = row.content.indexOfFirst { item ->
            key.sectionKey == row.id && key.itemKey == item.id
        }
        if (contentIndex >= 0) {
            return WebHomeFocusTarget(
                rowIndex = rowIndex,
                contentIndex = contentIndex,
            )
        }
    }
    return null
}

internal fun WebBrowseFocusKey.matchesHomeContent(
    sectionKey: String,
    itemKey: String,
): Boolean {
    return destination == WebBrowseDestination.Home &&
        this.sectionKey == sectionKey &&
        this.itemKey == itemKey
}

private fun ContentModel.matches(key: WebBrowseFocusKey): Boolean {
    return toWebHomeFocusKey() == key
}

private const val UnsectionedHomeSectionKey = "home:unsectioned"
