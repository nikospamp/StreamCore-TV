package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.resources.Res
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_cast
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_overview
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailsOverview(
    content: ContentModel,
    modifier: Modifier = Modifier,
    headingStyle: TextStyle = MaterialTheme.typography.titleMedium,
    genresStyle: TextStyle = MaterialTheme.typography.labelLarge,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    castHeadingStyle: TextStyle = MaterialTheme.typography.titleSmall,
    castStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { genre -> genre.name }
    }
    val castText = remember(content.cast) {
        content.cast.joinToString(separator = " · ") { cast ->
            cast.characterName?.let { characterName ->
                "${cast.name} as $characterName"
            } ?: cast.name
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Overview),
    ) {
        Text(
            text = stringResource(Res.string.details_overview),
            style = headingStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (genreText.isNotBlank()) {
            Text(
                text = genreText,
                style = genresStyle,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = content.description,
            style = descriptionStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (castText.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny)) {
                Text(
                    text = stringResource(Res.string.details_cast),
                    style = castHeadingStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = castText,
                    style = castStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun DetailsOverviewDarkPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsOverview(content = DetailsPreviewData.content)
    }
}

@Preview(widthDp = 360)
@Composable
private fun DetailsOverviewPreview() {
    StreamCoreTheme(darkTheme = false) {
        DetailsOverview(content = DetailsPreviewData.content)
    }
}

@Preview(widthDp = 360)
@Composable
private fun DetailsOverviewWithoutMetadataPreview() {
    StreamCoreTheme {
        DetailsOverview(
            content = DetailsPreviewData.content.copy(
                genres = emptyList(),
                cast = emptyList(),
            ),
        )
    }
}
