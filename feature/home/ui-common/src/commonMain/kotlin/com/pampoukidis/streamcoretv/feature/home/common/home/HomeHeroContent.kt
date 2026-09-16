package com.pampoukidis.streamcoretv.feature.home.common.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.heroMetadata
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData

/** Portable hero copy; platform wrappers provide localized labels, insets, and their action controls. */
@Composable
fun HomeHeroContent(
    content: ContentModel,
    featuredLabel: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    titleStyle: TextStyle = MaterialTheme.typography.displayLarge,
    descriptionMaxLines: Int = 2,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier.padding(contentPadding),
    ) {
        Text(
            text = featuredLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = content.title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onArtwork,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = content.heroMetadata(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = content.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onArtwork.copy(alpha = 0.82f),
            maxLines = descriptionMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Preview
@Composable
private fun HomeHeroContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        HomeHeroContent(
            content = HomePreviewData.rows.first().content.first(),
            featuredLabel = "Featured movie",
            modifier = Modifier.width(StreamCoreDimens.Web.Home.HeroCopyMaxWidth),
            contentPadding = PaddingValues(StreamCoreDimens.Spacing.ExtraLarge),
        ) { }
    }
}
