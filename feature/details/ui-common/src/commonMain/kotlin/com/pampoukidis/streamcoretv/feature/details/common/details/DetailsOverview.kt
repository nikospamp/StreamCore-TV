package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags

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
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailsTestTags.Overview),
    ) {
        DetailsSynopsis(
            content = content,
            headingStyle = headingStyle,
            genresStyle = genresStyle,
            descriptionStyle = descriptionStyle,
        )
        DetailsCast(
            content = content,
            headingStyle = castHeadingStyle,
            castStyle = castStyle,
        )
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
