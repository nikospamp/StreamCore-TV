package com.pampoukidis.streamcoretv.feature.search.domain

object SearchQueryNormalizer {

    fun normalize(query: String): String {
        return query.trim().replace(WhitespaceRegex, " ")
    }

    fun isSearchable(query: String): Boolean {
        return normalize(query).length >= MinimumQueryLength
    }

    const val MinimumQueryLength = 3

    private val WhitespaceRegex = Regex("\\s+")
}
