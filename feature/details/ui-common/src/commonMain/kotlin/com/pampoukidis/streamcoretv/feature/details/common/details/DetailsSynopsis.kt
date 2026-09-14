package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.resources.Res
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_overview
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import org.jetbrains.compose.resources.stringResource

/** Shared text rendering; the platform owns expansion controls and reading interaction. */
@Composable
fun DetailsSynopsis(
    content: ContentModel,
    modifier: Modifier = Modifier,
    headingStyle: TextStyle = MaterialTheme.typography.titleMedium,
    genresStyle: TextStyle = MaterialTheme.typography.labelLarge,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    afterDescription: @Composable () -> Unit = {},
) {
    val genreText = remember(content.genres) {
        content.genres.joinToString(separator = " · ") { genre -> genre.name }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier,
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
            maxLines = maxLines,
            overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = onTextLayout,
        )
        afterDescription()
    }
}

@Preview(widthDp = 360)
@Composable
private fun DetailsSynopsisPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsSynopsis(content = DetailsPreviewData.content)
    }
}
