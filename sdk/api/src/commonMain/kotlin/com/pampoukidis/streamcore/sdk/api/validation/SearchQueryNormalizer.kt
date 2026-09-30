package com.pampoukidis.streamcore.sdk.api.validation

/** Pure query feedback shared with SDK search/history operations; it performs no request or history write. */
object SearchQueryNormalizer {

    /** Trims leading/trailing whitespace and collapses internal whitespace runs to one space. */
    fun normalize(query: String): String {
        return query.trim().replace(WhitespaceRegex, " ")
    }

    /** Applies normalization before testing the minimum query length. */
    fun isSearchable(query: String): Boolean {
        return normalize(query).length >= MinimumQueryLength
    }

    const val MinimumQueryLength = 3

    private val WhitespaceRegex = Regex("\\s+")
}
