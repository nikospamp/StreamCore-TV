package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBCategoryDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContentDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContributorDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBHomeLaneDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBLaneTemplateDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientBCatalogSource @Inject constructor() {

    private val catalog = listOf(
        content(
            id = "client-b-last-archive",
            title = "The Last Archive",
            description = "An archivist discovers a record that should not exist.",
            rating = 9,
            parentalRating = "13+",
            parentalLevel = 13,
            categories = listOf(
                ClientBCategoryDto(code = "mystery", displayName = "Mystery"),
                ClientBCategoryDto(code = "science-fiction", displayName = "Science Fiction"),
            ),
        ),
        content(
            id = "client-b-city-lights",
            title = "City Lights",
            description = "Five stories intersect over one night in a changing city.",
            rating = 8,
            parentalRating = "16+",
            parentalLevel = 16,
            categories = listOf(
                ClientBCategoryDto(code = "drama", displayName = "Drama"),
            ),
        ),
        content(
            id = "client-b-paper-rockets",
            title = "Paper Rockets",
            description = "Friends build a backyard mission control for a school competition.",
            rating = 8,
            parentalRating = "All",
            parentalLevel = 0,
            categories = listOf(
                ClientBCategoryDto(code = "family", displayName = "Family"),
                ClientBCategoryDto(code = "adventure", displayName = "Adventure"),
            ),
        ),
        content(
            id = "client-b-deep-current",
            title = "Deep Current",
            description = "Researchers map an unexplored current beneath the Atlantic.",
            rating = 7,
            parentalRating = "7+",
            parentalLevel = 7,
            categories = listOf(
                ClientBCategoryDto(code = "documentary", displayName = "Documentary"),
            ),
        ),
    )

    internal fun contentForProfile(profileId: String): List<ClientBContentDto> {
        if (profileId.contains("kids", ignoreCase = true)) {
            return catalog.filter { it.parentalLevel <= KIDS_MATURITY_LEVEL }
        }

        return catalog
    }

    internal fun homeLanes(content: List<ClientBContentDto>): List<ClientBHomeLaneDto> {
        return listOf(
            ClientBHomeLaneDto(
                laneId = "client-b-spotlight",
                title = "Spotlight",
                caption = "Featured for this profile",
                template = ClientBLaneTemplateDto.Spotlight,
                assets = content.take(3),
            ),
            ClientBHomeLaneDto(
                laneId = "client-b-ranking",
                title = "Top 10 today",
                caption = "Trending on Client B",
                template = ClientBLaneTemplateDto.Ranking,
                assets = content.sortedByDescending { it.ratingOutOfTen },
            ),
            ClientBHomeLaneDto(
                laneId = "client-b-for-you",
                title = "For you",
                caption = "Recommended from your viewing profile",
                template = ClientBLaneTemplateDto.Portrait,
                assets = content,
            ),
            ClientBHomeLaneDto(
                laneId = "client-b-latest",
                title = "Latest releases",
                caption = "Recently added to the catalog",
                template = ClientBLaneTemplateDto.Landscape,
                assets = content.sortedByDescending { it.releaseEpochMillis },
            ),
        )
    }

    private fun content(
        id: String,
        title: String,
        description: String,
        rating: Int,
        parentalRating: String,
        parentalLevel: Int,
        categories: List<ClientBCategoryDto>,
    ): ClientBContentDto {
        return ClientBContentDto(
            assetId = id,
            displayTitle = title,
            description = description,
            ratingOutOfTen = rating,
            parentalRating = parentalRating,
            parentalLevel = parentalLevel,
            portraitImage = "https://images.client-b.example/$id/portrait.jpg",
            landscapeImage = "https://images.client-b.example/$id/landscape.jpg",
            releaseEpochMillis = 1_711_929_600_000L,
            contributors = listOf(
                ClientBContributorDto(
                    id = "client-b-contributor-lead",
                    displayName = "Jordan Lee",
                    roleName = "Lead",
                    imageUrl = null,
                ),
            ),
            categories = categories,
        )
    }

    private companion object {
        const val KIDS_MATURITY_LEVEL = 7
    }
}