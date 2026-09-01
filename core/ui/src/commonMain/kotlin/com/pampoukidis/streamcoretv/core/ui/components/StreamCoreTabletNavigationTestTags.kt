package com.pampoukidis.streamcoretv.core.ui.components

object StreamCoreTabletNavigationTestTags {
    const val Rail = "tablet-navigation:rail"

    fun destination(name: String): String {
        return "tablet-navigation:${name.lowercase()}"
    }
}
