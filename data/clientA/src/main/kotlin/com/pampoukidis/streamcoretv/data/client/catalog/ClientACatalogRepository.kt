package com.pampoukidis.streamcoretv.data.client.catalog

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.data.client.model.ClientACastDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAContentDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAGenreDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAHomeRowDto
import com.pampoukidis.streamcoretv.data.client.model.ClientARowPresentationDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientACatalogRepository @Inject constructor() : HomeRepository {

    private val catalog = listOf(
        content(
            id = "client-a-orbit-fall",
            title = "Orbit Fall",
            synopsis = "A rescue crew races to stabilize a failing orbital station.",
            score = 9,
            maturityLabel = "PG-13",
            maturityRank = 13,
            genres = listOf(
                ClientAGenreDto(genreId = "science-fiction", label = "Science Fiction"),
                ClientAGenreDto(genreId = "thriller", label = "Thriller"),
            ),
        ),
        content(
            id = "client-a-northern-line",
            title = "Northern Line",
            synopsis = "A detective follows one final lead through a frozen border town.",
            score = 8,
            maturityLabel = "16",
            maturityRank = 16,
            genres = listOf(
                ClientAGenreDto(genreId = "drama", label = "Drama"),
                ClientAGenreDto(genreId = "thriller", label = "Thriller"),
            ),
        ),
        content(
            id = "client-a-little-comets",
            title = "Little Comets",
            synopsis = "Young explorers build a telescope that changes their summer.",
            score = 8,
            maturityLabel = "All",
            maturityRank = 0,
            genres = listOf(
                ClientAGenreDto(genreId = "family", label = "Family"),
            ),
        ),
        content(
            id = "client-a-wild-coast",
            title = "Wild Coast",
            synopsis = "A close look at life along one of the world's roughest coastlines.",
            score = 7,
            maturityLabel = "7",
            maturityRank = 7,
            genres = listOf(
                ClientAGenreDto(genreId = "documentary", label = "Documentary"),
            ),
        ),
    )

    override suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>> {
        if (profileId.isBlank()) {
            return catalogFailure("PROFILE_ID_REQUIRED")
        }

        val profileCatalog = if (profileId.contains("kids", ignoreCase = true)) {
            catalog.filter { it.maturityRank <= KIDS_MATURITY_RANK }
        } else {
            catalog
        }
        return AppResult.Success(
            homeRows(profileCatalog)
                .filter { it.items.isNotEmpty() }
                .map { it.toModel() },
        )
    }

    private fun homeRows(content: List<ClientAContentDto>): List<ClientAHomeRowDto> {
        return listOf(
            ClientAHomeRowDto(
                rowId = "client-a-featured",
                heading = "Featured",
                subheading = "Selected for your profile",
                presentation = ClientARowPresentationDto.HeroCarousel,
                items = content.take(3),
            ),
            ClientAHomeRowDto(
                rowId = "client-a-top-ten",
                heading = "Top 10 today",
                subheading = "Most watched on Client A",
                presentation = ClientARowPresentationDto.RankedShelf,
                items = content.sortedByDescending { it.score },
            ),
            ClientAHomeRowDto(
                rowId = "client-a-recommended",
                heading = "Recommended for you",
                subheading = "Based on your active profile",
                presentation = ClientARowPresentationDto.PortraitShelf,
                items = content,
            ),
            ClientAHomeRowDto(
                rowId = "client-a-new",
                heading = "New this week",
                subheading = "Recently added",
                presentation = ClientARowPresentationDto.LandscapeShelf,
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
        genres: List<ClientAGenreDto>,
    ): ClientAContentDto {
        return ClientAContentDto(
            contentId = id,
            name = title,
            synopsis = synopsis,
            score = score,
            maturityLabel = maturityLabel,
            maturityRank = maturityRank,
            posterUrl = "https://images.client-a.example/$id/poster.jpg",
            heroImageUrl = "https://images.client-a.example/$id/backdrop.jpg",
            publishedAtEpochMs = 1_711_929_600_000L,
            cast = listOf(
                ClientACastDto(
                    personId = "client-a-cast-lead",
                    fullName = "Alex Morgan",
                    character = "Lead",
                    portraitUrl = null,
                ),
            ),
            genres = genres,
        )
    }

    private fun catalogFailure(backendCode: String): AppResult.Failure {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = GET_HOME_ROWS_OPERATION,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private companion object {
        const val CLIENT = "clientA"
        const val GET_HOME_ROWS_OPERATION = "getHomeRows"
        const val KIDS_MATURITY_RANK = 7
    }
}
