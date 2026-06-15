package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCastDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbContentDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbHomeRowDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRowPresentationDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbCatalogSource @Inject constructor() {

    private val catalog = listOf(
        content(
            id = "tmdb-orbit-fall",
            title = "Orbit Fall",
            synopsis = "A rescue crew races to stabilize a failing orbital station.",
            score = 9,
            maturityLabel = "PG-13",
            maturityRank = 13,
            genres = listOf(
                TmdbGenreDto(genreId = "science-fiction", label = "Science Fiction"),
                TmdbGenreDto(genreId = "thriller", label = "Thriller"),
            ),
        ),
        content(
            id = "tmdb-northern-line",
            title = "Northern Line",
            synopsis = "A detective follows one final lead through a frozen border town.",
            score = 8,
            maturityLabel = "16",
            maturityRank = 16,
            genres = listOf(
                TmdbGenreDto(genreId = "drama", label = "Drama"),
                TmdbGenreDto(genreId = "thriller", label = "Thriller"),
            ),
        ),
        content(
            id = "tmdb-little-comets",
            title = "Little Comets",
            synopsis = "Young explorers build a telescope that changes their summer.",
            score = 8,
            maturityLabel = "All",
            maturityRank = 0,
            genres = listOf(
                TmdbGenreDto(genreId = "family", label = "Family"),
            ),
        ),
        content(
            id = "tmdb-wild-coast",
            title = "Wild Coast",
            synopsis = "A close look at life along one of the world's roughest coastlines.",
            score = 7,
            maturityLabel = "7",
            maturityRank = 7,
            genres = listOf(
                TmdbGenreDto(genreId = "documentary", label = "Documentary"),
            ),
        ),
    )

    internal fun contentForProfile(profileId: String): List<TmdbContentDto> {
        if (profileId.contains("kids", ignoreCase = true)) {
            return catalog.filter { it.maturityRank <= KIDS_MATURITY_RANK }
        }

        return catalog
    }

    internal fun homeRows(content: List<TmdbContentDto>): List<TmdbHomeRowDto> {
        return listOf(
            TmdbHomeRowDto(
                rowId = "tmdb-featured",
                heading = "Featured",
                subheading = "Selected for your profile",
                presentation = TmdbRowPresentationDto.HeroCarousel,
                items = content.take(3),
            ),
            TmdbHomeRowDto(
                rowId = "tmdb-top-ten",
                heading = "Top 10 today",
                subheading = "Most watched on TMDB",
                presentation = TmdbRowPresentationDto.RankedShelf,
                items = content.sortedByDescending { it.score },
            ),
            TmdbHomeRowDto(
                rowId = "tmdb-recommended",
                heading = "Recommended for you",
                subheading = "Based on your active profile",
                presentation = TmdbRowPresentationDto.PortraitShelf,
                items = content,
            ),
            TmdbHomeRowDto(
                rowId = "tmdb-new",
                heading = "New this week",
                subheading = "Recently added",
                presentation = TmdbRowPresentationDto.LandscapeShelf,
                items = content.sortedByDescending { it.publishedAtEpochMs },
            ),
        )
    }

    private fun content(
        id: String,
        title: String,
        synopsis: String,
        score: Int,
        maturityLabel: String,
        maturityRank: Int,
        genres: List<TmdbGenreDto>,
    ): TmdbContentDto {
        return TmdbContentDto(
            contentId = id,
            name = title,
            synopsis = synopsis,
            score = score,
            maturityLabel = maturityLabel,
            maturityRank = maturityRank,
            posterUrl = "https://images.tmdb.example/$id/poster.jpg",
            heroImageUrl = "https://images.tmdb.example/$id/backdrop.jpg",
            publishedAtEpochMs = 1_711_929_600_000L,
            cast = listOf(
                TmdbCastDto(
                    personId = "tmdb-cast-lead",
                    fullName = "Alex Morgan",
                    character = "Lead",
                    portraitUrl = null,
                ),
            ),
            genres = genres,
        )
    }

    private companion object {
        const val KIDS_MATURITY_RANK = 7
    }
}
