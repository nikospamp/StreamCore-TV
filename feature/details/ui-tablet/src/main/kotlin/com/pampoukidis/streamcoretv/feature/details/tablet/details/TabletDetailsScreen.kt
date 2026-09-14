package com.pampoukidis.streamcoretv.feature.details.tablet.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.tracing.benchmarkReadiness
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreDelayedEntrance
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsCast
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsPanoramaHero
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsRecommendationArtwork
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsSynopsis
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.resources.Res
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_hide_cast
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_read_more
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_show_cast
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_show_less
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.common.touch.details.DetailsTouchActions
import com.pampoukidis.streamcoretv.feature.details.common.touch.details.DetailsTouchTopControls
import com.pampoukidis.streamcoretv.feature.details.tablet.R
import org.jetbrains.compose.resources.stringResource as commonStringResource

@Composable
fun TabletDetailsScreen(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
    sourceArtworkUrl: String? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(DetailsTestTags.Root)
            .benchmarkReadiness("details-tablet", !state.isLoading && state.content != null),
    ) {
        DetailsBody(
            state = state,
            onAction = onAction,
            sharedElementScope = sharedElementScope,
            sourceArtworkUrl = sourceArtworkUrl,
        )
    }
}

@Composable
private fun DetailsBody(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope?,
    sourceArtworkUrl: String?,
) {
    val content = state.content

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && content == null -> CircularProgressIndicator(
                modifier = Modifier.testTag(DetailsTestTags.Loading),
            )
            content == null -> Text(
                text = "Unable to load details.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DetailsTestTags.Error),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = StreamCoreDimens.Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .testTag(DetailsTestTags.Content),
            ) {
                item(key = "summary", contentType = "summary") {
                    SummarySection(
                        state = state,
                        onAction = onAction,
                        sharedElementScope = sharedElementScope,
                        sourceArtworkUrl = sourceArtworkUrl,
                    )
                }
                item(key = "recommendations", contentType = "recommendations") {
                    StreamCoreDelayedEntrance(
                        visibleKey = "${content.id}:recommendations",
                        delayMillis = RecommendationsEntranceDelayMillis,
                    ) {
                        RecommendationsRow(
                            recommendations = state.recommendations,
                            onAction = onAction,
                        )
                    }
                }
            }
        }

        if (content == null) {
            DetailsTouchTopControls(
                isLoading = state.isLoading,
                onBack = { onAction(DetailsAction.BackSelected) },
                onRefresh = { onAction(DetailsAction.Refresh) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                        vertical = StreamCoreDimens.Spacing.Medium,
                    ),
            )
        }
    }
}

@Composable
private fun SummarySection(
    state: DetailsUiState,
    onAction: (DetailsAction) -> Unit,
    sharedElementScope: StreamCoreSharedElementScope?,
    sourceArtworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    val content = requireNotNull(state.content)

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier.fillMaxWidth(),
    ) {
        DetailsPanoramaHero(
            content = content,
            sharedElementScope = sharedElementScope,
            sourceArtworkUrl = sourceArtworkUrl,
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                vertical = StreamCoreDimens.Spacing.ExtraLarge,
            ),
            modifier = Modifier.heightIn(min = StreamCoreDimens.Tablet.Details.PanoramaHeroHeight),
            bottomContent = {
                DetailsActions(
                    hasResumableProgress = state.hasResumableProgress,
                    isTrailerAvailable = content.trailers.isNotEmpty(),
                    isLibraryAvailable = state.isLibraryAvailable,
                    isLiked = state.isLiked,
                    isInMyList = state.isInMyList,
                    isLikeMutationPending = state.isLikeMutationPending,
                    isMyListMutationPending = state.isMyListMutationPending,
                    onPlayClick = { onAction(DetailsAction.PlaySelected) },
                    onTrailerClick = { onAction(DetailsAction.TrailerSelected) },
                    onLikeClick = { onAction(DetailsAction.LikeToggled) },
                    onMyListClick = { onAction(DetailsAction.MyListToggled) },
                )
            },
            overlay = {
                DetailsTouchTopControls(
                    isLoading = state.isLoading,
                    onBack = { onAction(DetailsAction.BackSelected) },
                    onRefresh = { onAction(DetailsAction.Refresh) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(
                            horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                            vertical = StreamCoreDimens.Spacing.Medium,
                        ),
                )
            },
        )
        StreamCoreDelayedEntrance(
            visibleKey = content.id,
            delayMillis = MetadataEntranceDelayMillis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
        ) {
            DetailsReadingBand(content = content)
        }
    }
}

@Composable
private fun DetailsActions(
    hasResumableProgress: Boolean,
    isTrailerAvailable: Boolean,
    isLibraryAvailable: Boolean,
    isLiked: Boolean,
    isInMyList: Boolean,
    isLikeMutationPending: Boolean,
    isMyListMutationPending: Boolean,
    onPlayClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMyListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        StreamCoreButton(
            text = stringResource(
                if (hasResumableProgress) R.string.details_action_resume else R.string.details_action_play,
            ),
            onClick = onPlayClick,
            enabled = true,
            leadingIcon = { StreamCorePlayIcon() },
            modifier = Modifier
                .width(StreamCoreDimens.Tablet.Details.PlayButtonWidth)
                .testTag(DetailsTestTags.PlayButton),
        )
        DetailsTouchActions(
            isLibraryAvailable = isLibraryAvailable,
            isLiked = isLiked,
            isInMyList = isInMyList,
            isLikeMutationPending = isLikeMutationPending,
            isMyListMutationPending = isMyListMutationPending,
            isTrailerAvailable = isTrailerAvailable,
            onLikeClick = onLikeClick,
            onMyListClick = onMyListClick,
            onTrailerClick = onTrailerClick,
            contentColor = MaterialTheme.colorScheme.onArtwork,
            selectedContentColor = MaterialTheme.colorScheme.onArtwork,
            modifier = Modifier.widthIn(max = DetailsActionsMaxWidth),
        )
    }
}

@Composable
private fun DetailsReadingBand(
    content: ContentModel,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Overview),
    ) {
        ExpandableSynopsis(
            content = content,
            modifier = Modifier.weight(2f),
        )
        DetailsExpandableCast(
            content = content,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExpandableSynopsis(
    content: ContentModel,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(content.id, content.description) { mutableStateOf(false) }
    var overflows by remember(content.id, content.description) { mutableStateOf(false) }

    Box(modifier = modifier) {
        DetailsSynopsis(
            content = content,
            headingStyle = MaterialTheme.typography.headlineSmall,
            maxLines = if (expanded) Int.MAX_VALUE else CollapsedReadingLines,
            onTextLayout = { result ->
                if (!expanded) {
                    overflows = result.hasVisualOverflow
                }
            },
            afterDescription = {
                if (expanded || overflows) {
                    StreamCoreTextButton(
                        text = commonStringResource(
                            if (expanded) Res.string.details_show_less else Res.string.details_read_more,
                        ),
                        onClick = { expanded = !expanded },
                        enabled = true,
                    )
                }
            },
            modifier = Modifier.widthIn(max = StreamCoreDimens.Tablet.Details.SynopsisMaxWidth),
        )
    }
}

@Composable
private fun DetailsExpandableCast(
    content: ContentModel,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(content.id, content.cast) { mutableStateOf(false) }
    var overflows by remember(content.id, content.cast) { mutableStateOf(false) }

    DetailsCast(
        content = content,
        headingStyle = MaterialTheme.typography.headlineSmall,
        castStyle = MaterialTheme.typography.bodyLarge,
        maxLines = if (expanded) Int.MAX_VALUE else CollapsedReadingLines,
        onTextLayout = { result ->
            if (!expanded) {
                overflows = result.hasVisualOverflow
            }
        },
        afterCast = {
            if (expanded || overflows) {
                StreamCoreTextButton(
                    text = commonStringResource(
                        if (expanded) Res.string.details_hide_cast else Res.string.details_show_cast,
                    ),
                    onClick = { expanded = !expanded },
                    enabled = true,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun RecommendationsRow(
    recommendations: List<ContentModel>,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) {
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Recommendations),
    ) {
        Text(
            text = "More like this",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        ) {
            items(
                items = recommendations,
                key = { it.id },
                contentType = { "recommendation" },
            ) { content ->
                RecommendationCard(
                    content = content,
                    onClick = {
                        onAction(DetailsAction.RecommendationSelected(content, sourceArtworkUrl = content.poster))
                    },
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    content: ContentModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsRecommendationArtwork(
        content = content,
        modifier = modifier
            .width(StreamCoreDimens.Tablet.Details.RecommendationCardWidth)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag(DetailsTestTags.RecommendationPrefix + content.id),
    )
}

private const val MetadataEntranceDelayMillis = 80
private const val RecommendationsEntranceDelayMillis = 150
private const val CollapsedReadingLines = 3
private val DetailsActionsMaxWidth = StreamCoreDimens.Tablet.Details.ActionsMaxWidth

private fun tabletDetailsPreviewContent(): ContentModel {
    return DetailsPreviewData.content.copy(backdrop = null, poster = "")
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenDarkPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = tabletDetailsPreviewContent(),
                recommendations = DetailsPreviewData.recommendations.map { it.copy(backdrop = null, poster = "") },
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenResumeSelectedLightPreview() {
    StreamCoreTheme(darkTheme = false) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = tabletDetailsPreviewContent(),
                recommendations = DetailsPreviewData.recommendations.map { it.copy(backdrop = null, poster = "") },
                hasResumableProgress = true,
                isLibraryAvailable = true,
                isLiked = true,
                isInMyList = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenRetainedLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = true,
                content = tabletDetailsPreviewContent(),
                recommendations = DetailsPreviewData.recommendations.map { it.copy(backdrop = null, poster = "") },
                isLibraryAvailable = true,
                isLikeMutationPending = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenUnavailableActionsPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = tabletDetailsPreviewContent(),
                recommendations = DetailsPreviewData.recommendations.map { it.copy(backdrop = null, poster = "") },
                isLibraryAvailable = false,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenTrailerPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = tabletDetailsPreviewContent().copy(
                    trailers = listOf(
                        TrailerModel(
                            id = "official-trailer",
                            title = "Official trailer",
                            url = "https://example.test/trailer",
                        ),
                    ),
                ),
                recommendations = DetailsPreviewData.recommendations.map { it.copy(backdrop = null, poster = "") },
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletDetailsScreenLongTitlePreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = tabletDetailsPreviewContent().copy(
                    title = "The Extraordinary Adventures Beyond the Edge of the Known Universe",
                ),
                recommendations = DetailsPreviewData.recommendations.map { it.copy(backdrop = null, poster = "") },
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTablet
@Preview(name = "Compact tablet window", widthDp = 600, heightDp = 960, showBackground = true)
@Composable
private fun TabletDetailsScreenCompactLongReadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletDetailsScreen(
            state = DetailsUiState(
                isLoading = false,
                content = tabletDetailsPreviewContent().copy(
                    description = "A rescue crew races to stabilize a failing orbital station. " +
                        "With communications lost and the next evacuation window closing, " +
                        "the crew follows a hidden signal through the debris field. " +
                        "Their search uncovers a forgotten mission and forces them to choose " +
                        "between returning home and rescuing the people left behind.",
                ),
                isLibraryAvailable = true,
            ),
            onAction = {},
        )
    }
}
